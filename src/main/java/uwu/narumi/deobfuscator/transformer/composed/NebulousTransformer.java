package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.CheatBreakerStaticArrayStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.hp888.HP888StaticArrayStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.nebulous.NebulousStringPoolTransform;
import uwu.narumi.deobfuscator.transformer.impl.radon.RadonStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class NebulousTransformer extends ComposedTransformer {
    @Override
    public List<Transformer> transformers() {
       return Arrays.asList(
               new NebulousStringPoolTransform()
       );
    }
}
