package com.totempopchams;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Zeichnet die Geister als durchsichtige, einfarbige Block-Figur
 * (Kopf, Körper, zwei Arme, zwei Beine), ganz ohne Skin.
 */
public final class GhostRenderer {

    private static final String MOD_ID = "totempopchams";

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/ghost"))
                    .build());

    private static final RenderPipeline PIPELINE_THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/ghost_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final StagedVertexBuffer BUFFER =
            new StagedVertexBuffer(() -> "Totem Pop Chams Buffer", RenderType.SMALL_BUFFER_SIZE);

    /** Skaliert Pixel des Spielermodells (32 Pixel hoch) auf 1,8 Blöcke. */
    private static final float PX = 1.8f / 32.0f;

    /** Wird zwischen Extraktion und Zeichnen weitergereicht (unveränderlich). */
    private static List<GhostManager.Snapshot> snapshot = List.of();

    private GhostRenderer() {
    }

    public static void extract(LevelExtractionContext context) {
        snapshot = GhostManager.snapshot();
    }

    public static void draw(LevelRenderContext context) {
        List<GhostManager.Snapshot> ghosts = snapshot;
        if (ghosts.isEmpty()) {
            return;
        }

        RenderPipeline pipeline = GhostConfig.throughWalls ? PIPELINE_THROUGH_WALLS : PIPELINE;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) {
            return;
        }
        PrimitiveTopology topology = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(
                format,
                topology,
                topology == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer builder = BUFFER.getVertexBuilder(draw);
        Matrix4fc pose = matrices.last().pose();

        float r = ((GhostConfig.color >> 16) & 0xFF) / 255.0f;
        float g = ((GhostConfig.color >> 8) & 0xFF) / 255.0f;
        float b = (GhostConfig.color & 0xFF) / 255.0f;

        for (GhostManager.Snapshot ghost : ghosts) {
            if (ghost.alpha() > 0.0f) {
                addGhost(pose, builder, ghost, r, g, b);
            }
        }

        matrices.popPose();

        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null) {
            execute(Minecraft.getInstance(), info, pipeline);
        }
        BUFFER.endFrame();
    }

    public static void close() {
        BUFFER.close();
    }

    // ------------------------------------------------------------------------------------------

    private static void addGhost(Matrix4fc pose, VertexConsumer out, GhostManager.Snapshot ghost,
                                 float r, float g, float b) {
        double yaw = Math.toRadians(ghost.yawDegrees());
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);
        float a = ghost.alpha();

        // Alle Maße in Pixeln des Spielermodells: x = links/rechts, y = hoch, z = vorne/hinten.
        // Beine
        addBox(pose, out, ghost, cos, sin, -4, 0, 0, 12, -2, 2, r, g, b, a);
        addBox(pose, out, ghost, cos, sin, 0, 4, 0, 12, -2, 2, r, g, b, a);
        // Körper
        addBox(pose, out, ghost, cos, sin, -4, 4, 12, 24, -2, 2, r, g, b, a);
        // Arme
        addBox(pose, out, ghost, cos, sin, -8, -4, 12, 24, -2, 2, r, g, b, a);
        addBox(pose, out, ghost, cos, sin, 4, 8, 12, 24, -2, 2, r, g, b, a);
        // Kopf
        addBox(pose, out, ghost, cos, sin, -4, 4, 24, 32, -4, 4, r, g, b, a);
    }

    private static void addBox(Matrix4fc pose, VertexConsumer out, GhostManager.Snapshot ghost,
                               float cos, float sin,
                               float x0, float x1, float y0, float y1, float z0, float z1,
                               float r, float g, float b, float a) {
        // 8 Eckpunkte; Index = ix | (iy << 1) | (iz << 2)
        float[][] p = new float[8][3];
        for (int i = 0; i < 8; i++) {
            float lx = ((i & 1) == 0 ? x0 : x1) * PX;
            float ly = ((i & 2) == 0 ? y0 : y1) * PX;
            float lz = ((i & 4) == 0 ? z0 : z1) * PX;
            p[i][0] = ghost.x() + lx * cos - lz * sin;
            p[i][1] = ghost.y() + ly;
            p[i][2] = ghost.z() + lx * sin + lz * cos;
        }

        face(pose, out, p, 4, 5, 7, 6, r, g, b, a); // vorne
        face(pose, out, p, 1, 0, 2, 3, r, g, b, a); // hinten
        face(pose, out, p, 0, 4, 6, 2, r, g, b, a); // links
        face(pose, out, p, 5, 1, 3, 7, r, g, b, a); // rechts
        face(pose, out, p, 6, 7, 3, 2, r, g, b, a); // oben
        face(pose, out, p, 0, 1, 5, 4, r, g, b, a); // unten
    }

    private static void face(Matrix4fc pose, VertexConsumer out, float[][] p,
                             int i0, int i1, int i2, int i3,
                             float r, float g, float b, float a) {
        out.addVertex(pose, p[i0][0], p[i0][1], p[i0][2]).setColor(r, g, b, a);
        out.addVertex(pose, p[i1][0], p[i1][1], p[i1][2]).setColor(r, g, b, a);
        out.addVertex(pose, p[i2][0], p[i2][1], p[i2][2]).setColor(r, g, b, a);
        out.addVertex(pose, p[i3][0], p[i3][1], p[i3][2]).setColor(r, g, b, a);
    }

    private static void execute(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) {
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> MOD_ID + " ghost rendering", colorTexture, Optional.empty(),
                        mainTarget.getDepthTextureView(), OptionalDouble.empty())) {

            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());

            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}
