import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.composed.CaesiumTransformer;
import uwu.narumi.deobfuscator.transformer.composed.NebulousTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("test", "Evaluator-obf.jar"))
                .output(Path.of("test", "Evaluator-deobf.jar"))
                .transformers(
                        new NebulousTransformer()
                )
//                .normalize() // this is buggy
                .classReaderFlags(ClassReader.EXPAND_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_FRAMES)
                .consoleDebug()
                .build()
                .start();

    }
}