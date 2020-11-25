package com.pinball3d.effectinfo;

import java.util.Collection;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.pinball3d.effectinfo.api.JEEAPI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Render {
	public static ResourceLocation TEXTURE = new ResourceLocation("effectinfo:textures/gui/texture.png");
	public static ResourceLocation INV = new ResourceLocation("textures/gui/container/inventory.png");

	@SubscribeEvent
	public static void onRenderScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
		if (event.getGui() instanceof DisplayEffectsScreen) {
			DisplayEffectsScreen screen = (DisplayEffectsScreen) event.getGui();
			Collection<EffectInstance> collection = Minecraft.getInstance().player.getActivePotionEffects();
			if (collection.isEmpty()) {
				return;
			}
			int x = screen.getGuiLeft() - 124;
			int y = screen.getGuiTop();
			if (event.getMouseX() - x < 0 || event.getMouseX() - x > 120) {
				return;
			}
			int l = 33;
			if (collection.size() > 5) {
				l = 132 / (collection.size() - 1);
			}
			for (EffectInstance e : Ordering.natural().sortedCopy(collection)) {
				Effect potion = e.getPotion();
				if (!potion.shouldRender(e)) {
					continue;
				}
				if (event.getMouseY() - y > 0 && event.getMouseY() - y < l && canRender(e)) {
					render(e, x, y, screen, event.getMatrixStack());
					return;
				}
				y += l;
			}
		}
	}

	public static boolean canRender(EffectInstance e) {
		for (int i = 0; i < 5; i++) {
			String key = e.getEffectName() + "." + i + ".desc";
			if (I18n.hasKey(key)) {
				return true;
			}
		}
		return false;
	}

	public static void render(EffectInstance effect, int x, int y, DisplayEffectsScreen screen, MatrixStack matrix) {
		Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		FontRenderer fr = Minecraft.getInstance().fontRenderer;
		int amp = effect.getAmplifier();
		String[] desc = new String[5];
		int max = 0;
		for (int i = 0; i < 5; i++) {
			String key = effect.getEffectName() + "." + i + ".desc";
			String s = JEEAPI.getDesc(key);
			if (s != null && !s.isEmpty()) {
				desc[i] = s;
				max = i;
			} else if (I18n.hasKey(key)) {
				desc[i] = I18n.format(key);
				max = i;
			}
		}
		int yOffset = y + 30;
		int yOffsetTemp = 0;
		for (int i = 0; i <= max; i++) {
			String s = desc[i];
			if (s != null) {
				int yPos = yOffsetTemp == 0 ? yOffset : yOffsetTemp;
				int height = yPos + fr.getWordWrappedHeight(s, 85) - yOffset;
				height = height < 16 ? 16 : height;
				yOffsetTemp = 0;
				yOffset += height + 9;
			} else {
				if (yOffsetTemp == 0) {
					yOffsetTemp = yOffset;
				}
				yOffset += 20;
			}
		}
		drawRect(matrix, x, y, yOffset - y);
		yOffset = y + 30;
		yOffsetTemp = 0;
		for (int i = 0; i <= max; i++) {
			String s = desc[i];
			if (s != null) {
				int yPos = yOffsetTemp == 0 ? yOffset : yOffsetTemp;
				renderSplixString(fr, new StringTextComponent(s), x + 30, yPos, 85, 0xFFFFFF);
				int height = yPos + fr.getWordWrappedHeight(s, 85) - yOffset;
				if (yPos != y + 30) {
					AbstractGui.fill(matrix, x + 7, yPos - 5, x + 115, yPos - 4, 0xFF7F7F7F);
				}
				Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
				AbstractGui.blit(matrix, x + 10, yOffset, amp == i ? 138 : 120, i * 16, 18, 16, 256, 256);
				height = height < 16 ? 16 : height;
				yOffsetTemp = 0;
				yOffset += height + 9;
			} else {
				if (yOffsetTemp == 0) {
					yOffsetTemp = yOffset;
				}
				if (i <= max) {
					Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
					AbstractGui.blit(matrix, x + 10, yOffset, amp == i ? 138 : 120, i * 16, 18, 16, 256, 256);
				}
				yOffset += 20;
			}
		}
		Effect potion = effect.getPotion();
		TextureAtlasSprite textureatlassprite = Minecraft.getInstance().getPotionSpriteUploader().getSprite(potion);
		Minecraft.getInstance().getTextureManager()
				.bindTexture(textureatlassprite.getAtlasTexture().getTextureLocation());
		AbstractGui.blit(matrix, x + 6, y + 7, screen.getBlitOffset(), 18, 18, textureatlassprite);
		effect.renderInventoryEffect(screen, matrix, x, y, screen.getBlitOffset());
		if (potion.shouldRenderInvText(effect)) {
			String s = I18n.format(effect.getPotion().getName());
			if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
				s = s + ' ' + I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
			}
			fr.drawStringWithShadow(matrix, s, x + 28, y + 6, 0xFFFFFF);
			String s1 = EffectUtils.getPotionDurationString(effect, 1.0F);
			fr.drawStringWithShadow(matrix, s1, x + 28, y + 16, 0x7F7F7F);
		}
	}

	private static void drawRect(MatrixStack matrix, int x, int y, int height) {
		Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		AbstractGui.blit(matrix, x, y, 0, 0, 120, height - 4, 256, 256);
		AbstractGui.blit(matrix, x, y + height - 4, 0, 252, 120, 4, 256, 256);
	}

	private static void renderSplixString(FontRenderer fr, ITextProperties text, int x, int y, int maxLength,
			int color) {
		Matrix4f matrix4f = TransformationMatrix.identity().getMatrix();
		matrix4f.translate(new Vector3f(0, 0, 100));
		for (IReorderingProcessor ireorderingprocessor : fr.trimStringToWidth(text, maxLength)) {
			func_238415_a_(fr, ireorderingprocessor, x, y, color, matrix4f, false);
			y += 9;
		}
	}

	private static int func_238415_a_(FontRenderer fr, IReorderingProcessor reorderingProcessor, float x, float y,
			int color, Matrix4f matrix, boolean p_238415_6_) {
		IRenderTypeBuffer.Impl irendertypebuffer$impl = IRenderTypeBuffer
				.getImpl(Tessellator.getInstance().getBuffer());
		int i = fr.func_238416_a_(reorderingProcessor, x, y, color, p_238415_6_, matrix, irendertypebuffer$impl, false,
				0, 15728880);
		irendertypebuffer$impl.finish();
		return i;
	}
}
