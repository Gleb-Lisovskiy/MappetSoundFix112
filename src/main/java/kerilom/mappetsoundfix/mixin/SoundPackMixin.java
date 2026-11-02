package kerilom.mappetsoundfix.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kerilom.mappetsoundfix.CaseInsensitiveFiles;
import kerilom.mappetsoundfix.MappetSoundFix;

/**
 * Mappet 0.9 (1.12.2) resolves sound files with:
 *
 * <pre>
 * private File getFile(String path)
 * {
 *     return new File(this.folder, path.substring(7));
 * }
 * </pre>
 *
 * The game asks for lower-cased resource paths, so any file whose name
 * contains an upper-case letter is not found on Linux. This injection keeps
 * Mappet's own fast path and only falls back to a case-insensitive lookup when
 * the returned file does not exist.
 *
 * <p>The mixin is intentionally independent of Minecraft classes: the target
 * is addressed by name and the broadcaster only uses {@link File} and
 * {@link String}. This keeps it working across Mappet 0.9 builds and lets the
 * project compile without a full MCP environment.</p>
 */
@Mixin(targets = "mchorse.mappet.client.SoundPack", remap = false)
public abstract class SoundPackMixin
{
    @Shadow
    private File folder;

    @Inject(method = "getFile", at = @At("RETURN"), cancellable = true, remap = false)
    private void mappetsoundfix$resolveCaseInsensitive(String path, CallbackInfoReturnable<File> cir)
    {
        if (!MappetSoundFix.isCaseInsensitiveLookupEnabled())
        {
            return;
        }

        File file = cir.getReturnValue();

        if (file != null && file.exists())
        {
            return;
        }

        if (path == null || !path.startsWith("sounds/"))
        {
            return;
        }

        File resolved = CaseInsensitiveFiles.resolve(this.folder, path.substring(7));

        if (resolved == null)
        {
            if (MappetSoundFix.isLogResolvedFilesEnabled())
            {
                MappetSoundFix.LOGGER.warn("[MappetSoundFix] Could not resolve {}", path);
            }

            return;
        }

        cir.setReturnValue(resolved);

        if (MappetSoundFix.isLogResolvedFilesEnabled())
        {
            MappetSoundFix.LOGGER.info("[MappetSoundFix] {} -> {}", path, this.relative(resolved));
        }
    }

    private String relative(File file)
    {
        try
        {
            return this.folder.toPath().relativize(file.toPath()).toString().replace('\\', '/');
        }
        catch (RuntimeException ignored)
        {
            return file.getAbsolutePath();
        }
    }
}
