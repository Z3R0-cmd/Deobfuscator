package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.nebulous.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;

import java.util.Arrays;
import java.util.List;

public class NebulousTransformer extends ComposedTransformer {
    @Override
    public List<Transformer> transformers() {
       return Arrays.asList(
               new NebulousNumberPoolTransform(),
               //TODO String splitter
               //TODO String encryption
               new NebulousStringPoolTransform()
       );
    }
}
