package net.minecraft.src;

import cpw.mods.fml.client.FMLTextureFX;

public class TextureLavaFX extends FMLTextureFX {
    protected float[] field_1147_g = new float[256];
    protected float[] field_1146_h = new float[256];
    protected float[] field_1145_i = new float[256];
    protected float[] field_1144_j = new float[256];

    public TextureLavaFX() {
        super(Block.lavaMoving.blockIndexInTexture);
        this.setup();
    }

    @Override
    public void setup() {
        super.setup();
        field_1147_g = new float[tileSizeSquare];
        field_1146_h = new float[tileSizeSquare];
        field_1145_i = new float[tileSizeSquare];
        field_1144_j = new float[tileSizeSquare];
    }

    @Override
    public void onTick() {
        for(int x = 0; x < tileSizeBase; x++) {
            for(int y = 0; y < tileSizeBase; y++) {
                float var3 = 0.0F;
                int var4 = (int)(MathHelper.sin((float)y * (float) Math.PI * 2.0F / 16.0F) * 1.2F);
                int var5 = (int)(MathHelper.sin((float)x * (float) Math.PI * 2.0F / 16.0F) * 1.2F);

                for(int var6 = x - 1; var6 <= x + 1; ++var6) {
                    for(int var7 = y - 1; var7 <= y + 1; ++var7) {
                        int var8 = var6 + var4 & tileSizeMask;
                        int var9 = var7 + var5 & tileSizeMask;
                        var3 += this.field_1147_g[var8 + var9 * tileSizeBase];
                    }
                }

                this.field_1146_h[x + y * tileSizeBase] = var3 / 10.0F
                    + (
                    this.field_1145_i[(x + 0 & tileSizeMask) + (y + 0 & tileSizeMask) * tileSizeBase]
                        + this.field_1145_i[(x + 1 & tileSizeMask) + (y + 0 & tileSizeMask) * tileSizeBase]
                        + this.field_1145_i[(x + 1 & tileSizeMask) + (y + 1 & tileSizeMask) * tileSizeBase]
                        + this.field_1145_i[(x + 0 & tileSizeMask) + (y + 1 & tileSizeMask) * tileSizeBase]
                )
                    / 4.0F
                    * 0.8F;
                this.field_1145_i[x + y * tileSizeBase] += this.field_1144_j[x + y * tileSizeBase] * 0.01F;
                if (this.field_1145_i[x + y * tileSizeBase] < 0.0F) {
                    this.field_1145_i[x + y * tileSizeBase] = 0.0F;
                }

                this.field_1144_j[x + y * tileSizeBase] -= 0.06F;
                if (Math.random() < 0.005) {
                    this.field_1144_j[x + y * tileSizeBase] = 1.5F;
                }
            }
        }

        float[] var11 = this.field_1146_h;
        this.field_1146_h = this.field_1147_g;
        this.field_1147_g = var11;

        for(int i = 0; i < tileSizeSquare; i++) {
            float var13 = this.field_1147_g[i] * 2.0F;
            if (var13 > 1.0F) {
                var13 = 1.0F;
            }

            if (var13 < 0.0F) {
                var13 = 0.0F;
            }

            int var14 = (int)(var13 * 100.0F + 155.0F);
            int var15 = (int)(var13 * var13 * 255.0F);
            int var16 = (int)(var13 * var13 * var13 * var13 * 128.0F);
            if (this.anaglyphEnabled) {
                int var17 = (var14 * 30 + var15 * 59 + var16 * 11) / 100;
                int var18 = (var14 * 30 + var15 * 70) / 100;
                int var10 = (var14 * 30 + var16 * 70) / 100;
                var14 = var17;
                var15 = var18;
                var16 = var10;
            }

            this.imageData[i * 4 + 0] = (byte)var14;
            this.imageData[i * 4 + 1] = (byte)var15;
            this.imageData[i * 4 + 2] = (byte)var16;
            this.imageData[i * 4 + 3] = -1;
        }
    }
}
