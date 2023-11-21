package cpw.mods.fml.transformer;

import cpw.mods.fml.server.FMLServerHandler;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.minecraft.server.MinecraftServer;

@CTransformer(MinecraftServer.class)
public class MinecraftServerTransformer {
	@CInject(method = "startServer()V",
			target = @CTarget(value = "THROW", shift = CTarget.Shift.BEFORE, ordinal = 1),
			slice = @CSlice(from = @CTarget(
					value = "INVOKE",
					target = "<init>(Lnet/minecraft/server/MinecraftServer;)V"
			)), cancellable = true)
	private void onLoadComplete(InjectionCallback callback) {
		FMLServerHandler.instance().onLoadComplete();
	}
}
