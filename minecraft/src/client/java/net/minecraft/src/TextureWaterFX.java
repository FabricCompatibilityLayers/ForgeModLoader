package net.minecraft.src;

import cpw.mods.fml.client.FMLTextureFX;

public class TextureWaterFX extends FMLTextureFX {
    protected float[] red;
    protected float[] green;
    protected float[] blue;
    protected float[] alpha;
    private int tickCounter;

    public TextureWaterFX() {
        super(Block.waterMoving.blockIndexInTexture);
        this.setup();
    }

    @Override
    public void setup() {
        super.setup();
        this.red = new float[tileSizeSquare];
        this.green = new float[tileSizeSquare];
        this.blue = new float[tileSizeSquare];
        this.alpha = new float[tileSizeSquare];
        this.tickCounter = 0;
    }

    @Override
    public void onTick() {
        ++this.tickCounter;

        for(int x = 0; x < tileSizeBase; ++x) {
            for(int y = 0; y < tileSizeBase; ++y) {
                float var3 = 0.0F;

                for(int var4 = x - 1; var4 <= x + 1; ++var4) {
                    int var5 = var4 & 15;
                    int var6 = y & 15;
                    var3 += this.red[var5 + var6 * tileSizeBase];
                }

                this.green[x + y * tileSizeBase] = var3 / 3.3F + this.blue[x + y * tileSizeBase] * 0.8F;
            }
        }

        for(int x = 0; x < tileSizeBase; ++x) {
            for(int y = 0; y < tileSizeBase; ++y) {
                this.blue[x + y * tileSizeBase] += this.alpha[x + y * tileSizeBase] * 0.05F;
                if (this.blue[x + y * tileSizeBase] < 0.0F) {
                    this.blue[x + y * tileSizeBase] = 0.0F;
                }

                this.alpha[x + y * tileSizeBase] -= 0.1F;
                if (Math.random() < 0.05) {
                    this.alpha[x + y * tileSizeBase] = 0.5F;
                }
            }
        }

        float[] var13 = this.green;
        this.green = this.red;
        this.red = var13;

        for(int i = 0; i < tileSizeSquare; ++i) {
            float var16 = this.red[i];
            if (var16 > 1.0F) {
                var16 = 1.0F;
            }

            if (var16 < 0.0F) {
                var16 = 0.0F;
            }

            float var17 = var16 * var16;
            int var18 = (int)(32.0F + var17 * 32.0F);
            int var19 = (int)(50.0F + var17 * 64.0F);
            int var7 = 255;
            int var8 = (int)(146.0F + var17 * 50.0F);
            if (this.anaglyphEnabled) {
                int var9 = (var18 * 30 + var19 * 59 + var7 * 11) / 100;
                int var10 = (var18 * 30 + var19 * 70) / 100;
                int var11 = (var18 * 30 + var7 * 70) / 100;
                var18 = var9;
                var19 = var10;
                var7 = var11;
            }

            this.imageData[i * 4 + 0] = (byte)var18;
            this.imageData[i * 4 + 1] = (byte)var19;
            this.imageData[i * 4 + 2] = (byte)var7;
            this.imageData[i * 4 + 3] = (byte)var8;
        }
    }
}
