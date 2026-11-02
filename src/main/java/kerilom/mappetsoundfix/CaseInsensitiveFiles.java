package kerilom.mappetsoundfix;

import java.io.File;

/**
 * Resolves a relative path inside the Mappet sounds folder by comparing every
 * path component case-insensitively.
 *
 * <p>The exact-name check is tried first, so the common case (all lower-case
 * file names) costs the same as Mappet's original implementation. Only when a
 * component is not found does the directory get listed and scanned with
 * {@link String#equalsIgnoreCase(String)}.</p>
 *
 * <p>The resolver is deliberately free of Minecraft classes so it can be unit
 * tested on a normal JVM.</p>
 */
public final class CaseInsensitiveFiles
{
    private CaseInsensitiveFiles()
    {}

    /**
     * Resolves {@code relative} (for example {@code trs/loops/loop_servermid.ogg})
     * against {@code root}.
     *
     * @param root     the sound pack folder, usually {@code config/mappet/sounds}
     * @param relative a slash-separated path relative to {@code root}
     *
     * @return the existing file, or {@code null} when no component matches
     */
    public static File resolve(File root, String relative)
    {
        if (root == null || relative == null || relative.isEmpty())
        {
            return null;
        }

        File current = root;

        for (String part : relative.split("/"))
        {
            if (part.isEmpty() || part.equals("."))
            {
                continue;
            }

            /* Never allow the lookup to escape the sound pack folder. */
            if (part.equals(".."))
            {
                return null;
            }

            File exact = new File(current, part);

            if (exact.exists())
            {
                current = exact;

                continue;
            }

            File match = findIgnoreCase(current, part);

            if (match == null)
            {
                return null;
            }

            current = match;
        }

        return current.exists() ? current : null;
    }

    private static File findIgnoreCase(File directory, String name)
    {
        File[] children = directory.listFiles();

        if (children == null)
        {
            return null;
        }

        File match = null;

        for (File child : children)
        {
            if (!child.getName().equalsIgnoreCase(name))
            {
                continue;
            }

            /* Filesystem enumeration order is not stable. Prefer the
             * lexicographically smallest name so the same file is picked on
             * every launch, even if two files differ only by case. */
            if (match == null || child.getName().compareTo(match.getName()) < 0)
            {
                match = child;
            }
        }

        return match;
    }
}
