package uwu.narumi.deobfuscator.transformer.impl.nebulous;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;
import java.util.stream.Stream;

public class NebulousNumberPoolTransform extends Transformer {
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            final MethodNode clinit = ASMHelper.findClInit(classNode).orElse(null);
            if (clinit == null) return;

            final FieldNode numberPoolArr = findNumberPoolArray(classNode).orElse(null);
            if (numberPoolArr == null) return;

            IntInsnNode poolCreationNode = findPoolCreationNode(clinit).orElseThrow();

            final int poolSize = (int) ASMHelper.getNumber(poolCreationNode.getPrevious());

            final Map<Integer, Integer> numberMap = getNumbers(poolCreationNode);

            if (numberMap.isEmpty()) return;

            for (MethodNode methodNode : classNode.methods) {
                findPoolLookups(classNode, methodNode, numberPoolArr, GETSTATIC).forEach(fieldInsnNode -> {

                    int lookupIndex = ASMHelper.getInteger(fieldInsnNode.getNext());

                    int realValue = numberMap.get(lookupIndex);

                    methodNode.instructions.insertBefore(fieldInsnNode, ASMHelper.getNumber(realValue));

                    // Remove everything
                    methodNode.instructions.remove(fieldInsnNode.getNext().getNext()); // IALOAD
                    methodNode.instructions.remove(fieldInsnNode.getNext()); // ldc
                    methodNode.instructions.remove(fieldInsnNode);
                });
            }

            // Remove the pool from <clinit>
            findPoolLookups(classNode, clinit, numberPoolArr, PUTSTATIC).forEach(putStaticNode -> {
                int i = 0;
                final int size = clinit.instructions.indexOf(putStaticNode);

                while(i <= size) {
                    clinit.instructions.remove(clinit.instructions.get(0));
                    i++;
                }
            });

            // Nebulous will automatically generate a <clinit> method, so its nice to remove it if that is the case.
            if (clinit.instructions.size() == 1) classNode.methods.remove(clinit);
        });
    }

    /**
     * Finds all the lookups to the given pool, in the specified method.
     *
     * @param classNode  The class everything is in.
     * @param methodNode The method to search in.
     * @param pool       The pool.
     * @param opcode     The opcode the lookup has.
     * @return A Stream of FieldInsnNodes
     */
    public Stream<FieldInsnNode> findPoolLookups(ClassNode classNode, MethodNode methodNode, FieldNode pool, int opcode) {
        return Arrays.stream(methodNode.instructions.toArray())
                .filter(abstractInsnNode -> abstractInsnNode instanceof FieldInsnNode)
                .map(abstractInsnNode -> ((FieldInsnNode) abstractInsnNode))
                .filter(fieldInsnNode -> fieldInsnNode.getOpcode() == opcode)
                .filter(fieldInsnNode -> fieldInsnNode.owner.equals(classNode.name))
                .filter(fieldInsnNode -> fieldInsnNode.name.equals(pool.name))
                .filter(fieldInsnNode -> fieldInsnNode.desc.equals(pool.desc));
    }

    /**
     * Locates all indexes and keys from a clinit method.
     *
     * @param poolCreationNode An IntInsnNode that creates the pool array.
     * @return A map of indices to values.
     */
    private HashMap<Integer, Integer> getNumbers(final IntInsnNode poolCreationNode) {

        final HashMap<Integer, Integer> result = new HashMap<>();

        AbstractInsnNode current = poolCreationNode.getNext();

        while (current != null && current.getOpcode() != RETURN && current.getOpcode() != PUTSTATIC) {
            // DUP
            if (current.getOpcode() != DUP) throw new RuntimeException("Expected DUP!");
            current = current.getNext();

            // index
            int index = ASMHelper.getInteger(current);
            current = current.getNext();

            // value
            int value = ASMHelper.getInteger(current);
            current = current.getNext();

            // iastore
            if (current.getOpcode() != IASTORE) throw new RuntimeException("Expected IASTORE!");
            current = current.getNext();

            result.put(index, value);
        }

        return result;
    }

    /**
     * Locates the node used to initialize the array.
     *
     * @param clinit The &lt;clinit&gt; method.
     * @return An optional wrapping the node used to initialize the pool array.
     */
    private Optional<IntInsnNode> findPoolCreationNode(MethodNode clinit) {
        return Arrays.stream(clinit.instructions.toArray())

                .filter(abstractInsnNode -> abstractInsnNode instanceof IntInsnNode)
                .map(abstractInsnNode -> ((IntInsnNode) abstractInsnNode))

                .filter(intInsnNode -> intInsnNode.getOpcode() == NEWARRAY)
                .filter(intInsnNode -> intInsnNode.operand == T_INT)
                .findFirst();
    }

    /**
     * Find the number pool array field.
     *
     * @param classNode The class to look in
     * @return An Optional&lt;FieldNode&gt; of the number pool array field.
     */
    private Optional<FieldNode> findNumberPoolArray(ClassNode classNode) {
        return classNode.fields.stream()
                .filter(fieldNode -> fieldNode.name.equals("numberPoolArray")) // The field always has the same name: https://github.com/Tigermouthbear/nebulous/blob/8c6131d4fd98826b7b7b16573a1deceb3f842408/src/main/java/dev/tigr/nebulous/modifiers/constants/number/NumberPooler.kt#L11C49-L11C49
                .filter(fieldNode -> fieldNode.desc.equals("[I"))
                .findFirst();
    }
}
