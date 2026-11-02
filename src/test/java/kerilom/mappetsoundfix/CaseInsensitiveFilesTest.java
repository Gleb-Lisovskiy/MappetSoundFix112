package kerilom.mappetsoundfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CaseInsensitiveFilesTest
{
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void exactMatchIsReturnedAsIs() throws Exception
    {
        File root = this.temp.getRoot();
        File file = this.touch("lower.ogg");

        assertEquals(file, CaseInsensitiveFiles.resolve(root, "lower.ogg"));
    }

    @Test
    public void resolvesMixedCaseFileName() throws Exception
    {
        File root = this.temp.getRoot();
        File file = this.touch("loop_serverMid.ogg");

        assertEquals(file, CaseInsensitiveFiles.resolve(root, "loop_servermid.ogg"));
    }

    @Test
    public void resolvesEveryDirectoryLevel() throws Exception
    {
        File root = this.temp.getRoot();
        File file = this.touch("TRS/Loops/PuddleElectric2.ogg");

        assertEquals(file, CaseInsensitiveFiles.resolve(root, "trs/loops/puddleelectric2.ogg"));
    }

    @Test
    public void resolvesNamesWithSpacesAndCyrillic() throws Exception
    {
        File root = this.temp.getRoot();
        File file = this.touch("Звуки/взрыв Бочки.ogg");

        assertEquals(file, CaseInsensitiveFiles.resolve(root, "звуки/ВЗРЫВ БОЧКИ.ogg"));
    }

    @Test
    public void returnsNullWhenNothingMatches() throws Exception
    {
        this.temp.getRoot();

        assertNull(CaseInsensitiveFiles.resolve(this.temp.getRoot(), "missing/file.ogg"));
        assertNull(CaseInsensitiveFiles.resolve(this.temp.getRoot(), "missing.ogg"));
    }

    @Test
    public void refusesToEscapeTheRoot() throws Exception
    {
        this.touch("inside.ogg");
        this.temp.newFile("outside.ogg");

        assertNull(CaseInsensitiveFiles.resolve(this.temp.getRoot(), "../outside.ogg"));
        assertNull(CaseInsensitiveFiles.resolve(this.temp.getRoot(), "nested/../../outside.ogg"));
    }

    @Test
    public void ignoresEmptyComponents() throws Exception
    {
        File root = this.temp.getRoot();
        File file = this.touch("folder/file.ogg");

        assertEquals(file, CaseInsensitiveFiles.resolve(root, "folder//file.ogg"));
        assertEquals(file, CaseInsensitiveFiles.resolve(root, "folder/file.ogg/"));
    }

    @Test
    public void picksTheSameFileWhenOnlyCaseDiffers() throws Exception
    {
        File root = this.temp.getRoot();
        File upper = this.touch("Sound.ogg");
        File lower = this.touch("sound.ogg");

        /* Case-insensitive filesystems only keep one of the two files. */
        assumeTrue(!Files.isSameFile(upper.toPath(), lower.toPath()));

        File first = CaseInsensitiveFiles.resolve(root, "sound.ogg");
        File second = CaseInsensitiveFiles.resolve(root, "sound.ogg");

        assertEquals(first, second);

        File expected = upper.getName().compareTo(lower.getName()) <= 0 ? upper : lower;

        assertEquals(expected, CaseInsensitiveFiles.resolve(root, "SOUND.OGG"));
        assertTrue(first.exists());
    }

    private File touch(String relative) throws Exception
    {
        File file = new File(this.temp.getRoot(), relative);

        assertTrue(file.getParentFile().exists() || file.getParentFile().mkdirs());
        Files.write(file.toPath(), relative.getBytes(StandardCharsets.UTF_8));

        return file;
    }
}
