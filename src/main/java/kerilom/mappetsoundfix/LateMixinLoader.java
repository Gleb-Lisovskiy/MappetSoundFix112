package kerilom.mappetsoundfix;

import java.util.Collections;
import java.util.List;

import zone.rong.mixinbooter.ILateMixinLoader;

/**
 * Hands {@code mixins.mappetsoundfix.json} to MixinBooter during FML's
 * {@code CONSTRUCTING} state. This happens before
 * {@code mchorse.mappet.client.SoundPack} is loaded, so the mixin is applied
 * the first time Mappet touches the sound pack.
 *
 * <p>MixinBooter finds this class through FML's {@code ASMDataTable}: every
 * class that directly implements {@link ILateMixinLoader} is indexed as
 * annotation data, even without an annotation.</p>
 */
public final class LateMixinLoader implements ILateMixinLoader
{
    private static final List<String> CONFIGS =
        Collections.singletonList("mixins.mappetsoundfix.json");

    @Override
    public List<String> getMixinConfigs()
    {
        return CONFIGS;
    }
}
