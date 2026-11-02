package kerilom.mappetsoundfix;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Client-side addon that makes Mappet's custom sound pack work on
 * case-sensitive file systems (Linux, macOS).
 *
 * <p><b>The problem.</b> Mappet generates {@code sounds.json} from the real
 * file names, but Minecraft stores resource paths in lower case. Mappet then
 * resolves {@code sounds/trs/loops/loop_servermid.ogg} to
 * {@code config/mappet/sounds/trs/loops/loop_servermid.ogg}. On Windows
 * (NTFS is case-insensitive) that finds {@code loop_serverMid.ogg}; on Linux
 * it does not, and the sound is skipped with
 * {@code File ... does not exist, cannot add it to event ...}.</p>
 *
 * <p><b>The fix.</b> {@link kerilom.mappetsoundfix.mixin.SoundPackMixin}
 * keeps Mappet's exact-match lookup and only falls back to a case-insensitive
 * search when the file was not found. Files on disk are never renamed and
 * sound event names in scripts do not change.</p>
 *
 * @see CaseInsensitiveFiles
 */
@Mod(
    modid = MappetSoundFix.MODID,
    name = MappetSoundFix.NAME,
    version = MappetSoundFix.VERSION,
    clientSideOnly = true,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:mappet;required-after:mixinbooter"
)
public final class MappetSoundFix
{
    public static final String MODID = "mappetsoundfix";
    public static final String NAME = "Mappet Sound Fix";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;

    /** Read by the mixin on every lookup; defaults are safe if the config fails. */
    private static volatile boolean caseInsensitiveLookup = true;
    private static volatile boolean logResolvedFiles = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());

        try
        {
            config.load();

            config.setCategoryComment(CATEGORY_GENERAL,
                "Options for Mappet's custom sound pack (config/mappet/sounds).");

            caseInsensitiveLookup = config.getBoolean(
                "caseInsensitiveLookup",
                CATEGORY_GENERAL,
                true,
                "Find custom Mappet sounds whose file names contain upper-case letters on case-sensitive file systems.\n"
                    + "Disable this only if you want Mappet's original exact-match behaviour."
            );

            logResolvedFiles = config.getBoolean(
                "logResolvedFiles",
                CATEGORY_GENERAL,
                false,
                "Log every sound that was found through the case-insensitive fallback.\n"
                    + "Useful for diagnosing which file maps to which sound event."
            );
        }
        catch (Exception exception)
        {
            LOGGER.warn("[MappetSoundFix] Could not read the configuration, using defaults.", exception);
        }
        finally
        {
            if (config.hasChanged())
            {
                config.save();
            }
        }

        LOGGER.info(
            "[MappetSoundFix] Case-insensitive lookup: {}, resolved-file logging: {}.",
            caseInsensitiveLookup ? "enabled" : "disabled",
            logResolvedFiles ? "enabled" : "disabled"
        );
    }

    public static boolean isCaseInsensitiveLookupEnabled()
    {
        return caseInsensitiveLookup;
    }

    public static boolean isLogResolvedFilesEnabled()
    {
        return logResolvedFiles;
    }
}
