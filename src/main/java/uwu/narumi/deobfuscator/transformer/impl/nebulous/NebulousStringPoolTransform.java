package uwu.narumi.deobfuscator.transformer.impl.nebulous;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;

/**
 * Nebulous string pool transformer
 *
 * @author Z3R0
 */
public class NebulousStringPoolTransform extends Transformer {
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {

            final String arrayName = "stringPoolArray";

            System.out.println("Doing: " + classNode.name);
            int predictedAccessModifier = (((classNode.access & ACC_INTERFACE) != 0) ? ACC_PUBLIC : ACC_PRIVATE) | ((classNode.version > V1_8) ? 0 : ACC_FINAL) | ACC_STATIC;

            final FieldNode fieldNode = classNode.fields.stream()
                    .filter(fn -> fn.access == predictedAccessModifier)
                    .filter(fn -> fn.name.equals(arrayName))
                    .filter(fn -> fn.desc.equals("[Ljava/lang/String;"))
                    .findFirst().orElse(null);

            if (fieldNode == null) return;

            final MethodNode clinit = ASMHelper.findClInit(classNode).orElse(null);

            if (clinit == null) return;

            assert clinit.instructions.get(1) instanceof TypeInsnNode;

            // Nebulous' StringPooler inserts into the beginning of <clinit>, which makes this much easier.
            final int poolSize = ASMHelper.getInteger(clinit.instructions.get(0));

            Map<Integer, String> poolValues = new HashMap<>();

            for (int i = 0; i < poolSize; i++) {
                // Every item added to the string pool takes up 4 instructions.
                int expectedIndex = (4 * (i + 1));

                assert clinit.instructions.get(expectedIndex) instanceof LdcInsnNode;

                poolValues.put(i, ((LdcInsnNode) clinit.instructions.get(expectedIndex)).cst.toString());
            }

            // Replace lookups with string literals
            classNode.methods.forEach(methodNode -> {
                for (AbstractInsnNode instruction : methodNode.instructions) {
                    if(instruction instanceof FieldInsnNode && instruction.getOpcode() == GETSTATIC && ((FieldInsnNode) instruction).owner.equals(classNode.name) && ((FieldInsnNode) instruction).name.equals(arrayName)) {
                        // Get the lookup index
                        int index = ASMHelper.getInteger(instruction.getNext());

                        // Actually insert the constant
                        methodNode.instructions.insertBefore(instruction, new LdcInsnNode(poolValues.get(index)));

                        // Remove the lookup
                        methodNode.instructions.remove(instruction.getNext().getNext());
                        methodNode.instructions.remove(instruction.getNext());
                        methodNode.instructions.remove(instruction);
                    }
                }
            });

            // Remove the pool
            classNode.fields.remove(fieldNode);

            // Remove the first 2 instructions to make the array, all the item insertions, and then finally the putstatic.
            for(int i = 0; i < 3 + poolSize * 4; i++) {
                clinit.instructions.remove(clinit.instructions.get(0));
            }
        });
    }
}
