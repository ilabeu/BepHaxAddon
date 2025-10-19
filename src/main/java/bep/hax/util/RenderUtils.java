package bep.hax.util;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.Defines;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class RenderUtils {
    private static final VertexConsumerProvider.Immediate vertex = VertexConsumerProvider.immediate(new BufferAllocator(2048));
    private static final ShaderProgramKey POSITION_COLOR_KEY = new ShaderProgramKey(Identifier.ofVanilla("core/position_color"), VertexFormats.POSITION_COLOR, Defines.builder().build());
    public static boolean shouldRenderBox(ESPBlockData esp) {
        return switch (esp.shapeMode) {
            case Both -> esp.lineColor.a > 0 || esp.sideColor.a > 0;
            case Lines -> esp.lineColor.a > 0;
            case Sides -> esp.sideColor.a > 0;
        };
    }
    public static boolean shouldRenderTracer(ESPBlockData esp) {
        return esp.tracer && esp.tracerColor.a > 0;
    }
    public static void renderTracerTo(Render3DEvent event, @NotNull BlockPos pos, Color tracerColor) {
        Vec3d tracerPos = pos.toCenterPos();
        event.renderer.line(
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.x,
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.y,
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.z,
            tracerPos.x, tracerPos.y, tracerPos.z, tracerColor
        );
    }
    public static void renderBlock(Render3DEvent event, BlockPos pos, Color lineColor, Color sideColor, ShapeMode mode) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor, lineColor, mode, 0
        );
    }
    public static void renderBlock(Render3DEvent event, BlockPos pos, Color color) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color, color, ShapeMode.Lines, 0
        );
    }
    public static void rounded(MatrixStack stack, float x, float y, float w, float h, float radius, int p, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) ColorHelper.getAlpha(color) / 255.0F;
        float r = (float) ColorHelper.getRed(color) / 255.0F;
        float g = (float) ColorHelper.getGreen(color) / 255.0F;
        float b = (float) ColorHelper.getBlue(color) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.setShader(POSITION_COLOR_KEY);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        corner(x + w, y, radius, 360, p, r, g, b, a, bufferBuilder, matrix4f);
        corner(x, y, radius, 270, p, r, g, b, a, bufferBuilder, matrix4f);
        corner(x, y + h, radius, 180, p, r, g, b, a, bufferBuilder, matrix4f);
        corner(x + w, y + h, radius, 90, p, r, g, b, a, bufferBuilder, matrix4f);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }
    public static void corner(float x, float y, float radius, int angle, float p, float r, float g, float b, float a, BufferBuilder bufferBuilder, Matrix4f matrix4f) {
        for (float i = angle; i > angle - 90; i -= 90 / p) {
            bufferBuilder.vertex(matrix4f, (float) (x + Math.cos(Math.toRadians(i)) * radius), (float) (y + Math.sin(Math.toRadians(i)) * radius), 0).color(r, g, b, a);
        }
    }
    public static void text(String text, MatrixStack stack, float x, float y, int color) {
        mc.textRenderer.draw(text, x, y, color, false, stack.peek().getPositionMatrix(), vertex, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
        vertex.draw();
    }
    public static void quad(MatrixStack stack, float x, float y, float w, float h, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) ColorHelper.getAlpha(color) / 255.0F;
        float r = (float) ColorHelper.getRed(color) / 255.0F;
        float g = (float) ColorHelper.getGreen(color) / 255.0F;
        float b = (float) ColorHelper.getBlue(color) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.setShader(POSITION_COLOR_KEY);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x + w, y, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix4f, x, y, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix4f, x, y + h, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }
    public enum RenderMode {
        Solid,
        Fade,
        Pulse,
        Shrink
    }
}
