package io.github.foundationgames.animatica.util;

import io.github.foundationgames.animatica.mixin.NativeImageAccessor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;

public enum TextureUtil {;
    private static final long SIZEOF_INT = 4;

    /**
     * Copy a section of an image into another image
     *
     * @param src The source image to copy from
     * @param u The u coordinate on the source image to start the selection from
     * @param v The v coordinate on the source image to start the selection from
     * @param w The width of the selection area
     * @param h The height of the selection area
     * @param dest The destination image to copy to
     * @param du The u coordinate on the destination image to place the selection at
     * @param dv The v coordinate on the destination image to place the selection at
     */
    public static void copy(NativeImage src, int u, int v, int w, int h, NativeImage dest, int du, int dv) {
        w = MathHelper.clamp(dest.getWidth() - du, 0, w);
        h = MathHelper.clamp(dest.getHeight() - dv, 0, h);

        long srcPtr = ((NativeImageAccessor)(Object)src).getPointer();
        long dstPtr = ((NativeImageAccessor)(Object)dest).getPointer();

        for (int row = 0; row < h; row++) {
            int srcRowIdx = ((v + row) * src.getWidth()) + u;
            var srcRow = MemoryUtil.memIntBuffer(srcPtr + (srcRowIdx * SIZEOF_INT), w);

            int trgRowIdx = ((dv + row) * dest.getWidth()) + du;
            var trgRow = MemoryUtil.memIntBuffer(dstPtr + (trgRowIdx * SIZEOF_INT), w);

            MemoryUtil.memCopy(srcRow, trgRow);
        }
    }

    /**
     * Copy a blend between 2 sections on a source image to a destination image
     *
     * @param src The source image to copy from
     * @param u0 The u coordinate on the source image to start the first selection from
     * @param v0 The v coordinate on the source image to start the first selection from
     * @param u1 The u coordinate on the source image to start the second selection from
     * @param v1 The v coordinate on the source image to start the second selection from
     * @param w The width of the selection area
     * @param h The height of the selection area
     * @param dest The destination image to copy to
     * @param du The u coordinate on the destination image to place the selection at
     * @param dv The v coordinate on the destination image to place the selection at
     * @param blend The blend between the first selection from the source and the
     *              second (0 = solid first image, 1 = solid second image)
     */
    public static void blendCopy(NativeImage src, int u0, int v0, int u1, int v1, int w, int h, NativeImage dest, int du, int dv, float blend) {
        w = MathHelper.clamp(dest.getWidth() - du, 0, w);
        h = MathHelper.clamp(dest.getHeight() - dv, 0, h);

        long srcPtr = ((NativeImageAccessor)(Object)src).getPointer();
        long dstPtr = ((NativeImageAccessor)(Object)dest).getPointer();

        for (int row = 0; row < h; row++) {
            int src0RowIdx = ((v0 + row) * src.getWidth()) + u0;
            var src0Row = MemoryUtil.memIntBuffer(srcPtr + (src0RowIdx * SIZEOF_INT), w);

            int src1RowIdx = ((v1 + row) * src.getWidth()) + u1;
            var src1Row = MemoryUtil.memIntBuffer(srcPtr + (src1RowIdx * SIZEOF_INT), w);

            int trgRowIdx = ((dv + row) * dest.getWidth()) + du;
            var trgRow = MemoryUtil.memIntBuffer(dstPtr + (trgRowIdx * SIZEOF_INT), w);

            for (int col = 0; col < w; col++) {
                trgRow.put(col, lerpColor(src.getFormat(), src0Row.get(col), src1Row.get(col), blend));
            }
        }
    }

    public static int lerpColor(NativeImage.Format format, int c1, int c2, float delta) {
        int a1 = (c1 >> format.getAlphaOffset()) & 0xFF;
        int r1 = (c1 >> format.getRedOffset()) & 0xFF;
        int g1 = (c1 >> format.getGreenOffset()) & 0xFF;
        int b1 = (c1 >> format.getBlueOffset()) & 0xFF;

        int a2 = (c2 >> format.getAlphaOffset()) & 0xFF;
        int r2 = (c2 >> format.getRedOffset()) & 0xFF;
        int g2 = (c2 >> format.getGreenOffset()) & 0xFF;
        int b2 = (c2 >> format.getBlueOffset()) & 0xFF;

        // If the first or second color is transparent,
        // don't lerp any leftover rgb values and instead
        // only use those of the non-transparent color
        if (a1 <= 0) {
            r1 = r2;
            g1 = g2;
            b1 = b2;
        } else if (a2 <= 0) {
            r2 = r1;
            g2 = g1;
            b2 = b1;
        }

        int oa = MathHelper.lerp(delta, a1, a2);
        int or = MathHelper.lerp(delta, r1, r2);
        int og = MathHelper.lerp(delta, g1, g2);
        int ob = MathHelper.lerp(delta, b1, b2);

        return (oa << format.getAlphaOffset()) | (or << format.getRedOffset()) | (og << format.getGreenOffset()) | (ob << format.getBlueOffset());
    }
}
