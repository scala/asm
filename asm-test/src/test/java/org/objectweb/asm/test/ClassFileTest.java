package org.objectweb.asm.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link ClassFile}.
 *
 * @author Eric Bruneton
 */
public class ClassFileTest extends AsmTest {

  @Test
  public void testGetConstantPoolDump() {
    ClassFile classFile = new ClassFile(PrecompiledClass.JDK3_ALL_INSTRUCTIONS.getBytes());

    String constantPoolDump = classFile.getConstantPoolDump();

    assertTrue(constantPoolDump.contains("constant_pool: ConstantClassInfo jdk3/AllInstructions"));
  }

  /** Tests that newInstance() succeeds for each precompiled class. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testNewInstance_validClass(
      final PrecompiledClass classParameter, final Api apiParameter) {
    ClassFile classFile = new ClassFile(classParameter.getBytes());

    Executable newInstance = () -> classFile.newInstance();

    if (classParameter.isMoreRecentThanCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
  }

  /** Tests that newInstance() fails when trying to load an invalid or unverifiable class. */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testNewInstance_invalidClass(final InvalidClass invalidClass) {
    ClassFile classFile = new ClassFile(invalidClass.getBytes());

    Executable newInstance = () -> classFile.newInstance();

    assertThrows(ClassFormatException.class, newInstance);
  }

  /**
   * Tests that the static newInstance() method fails when trying to load an invalid or unverifiable
   * class.
   */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testStaticNewInstance_invalidClass(final InvalidClass invalidClass) {
    String className = invalidClass.toString();
    byte[] classContent = invalidClass.getBytes();

    Executable newInstance = () -> ClassFile.newInstance(className, classContent);

    switch (invalidClass) {
      case INVALID_ELEMENT_VALUE:
      case INVALID_TYPE_ANNOTATION_TARGET_TYPE:
      case INVALID_INSN_TYPE_ANNOTATION_TARGET_TYPE:
        break;
      case INVALID_BYTECODE_OFFSET:
      case INVALID_OPCODE:
      case INVALID_WIDE_OPCODE:
        assertThrows(VerifyError.class, newInstance);
        break;
      case INVALID_CLASS_VERSION:
      case INVALID_CONSTANT_POOL_INDEX:
      case INVALID_CONSTANT_POOL_REFERENCE:
      case INVALID_CP_INFO_TAG:
      case INVALID_STACK_MAP_FRAME_TYPE:
      case INVALID_VERIFICATION_TYPE_INFO:
        assertThrows(ClassFormatError.class, newInstance);
        break;
      default:
        fail("Unknown invalid class");
        break;
    }
  }

  @Test
  public void testEquals() {
    ClassFile classFile1 = new ClassFile(PrecompiledClass.JDK3_ALL_INSTRUCTIONS.getBytes());
    ClassFile classFile2 = new ClassFile(PrecompiledClass.JDK5_ALL_INSTRUCTIONS.getBytes());

    assertEquals(classFile1, classFile1);
    assertNotEquals(classFile1, classFile2);
    assertNotEquals(classFile1, new byte[0]);
    assertNotEquals(new byte[0], classFile1);
  }

  @Test
  public void testHashcodeAndToString_validClass() {
    PrecompiledClass precompiledClass = PrecompiledClass.JDK3_ALL_INSTRUCTIONS;
    ClassFile classFile = new ClassFile(precompiledClass.getBytes());

    assertNotEquals(0, classFile.hashCode());
    assertTrue(classFile.toString().contains(precompiledClass.getInternalName()));
  }

  @Test
  public void testHashcodeAndToString_invalidClass() {
    InvalidClass invalidClass = InvalidClass.INVALID_CLASS_VERSION;
    ClassFile classFile = new ClassFile(invalidClass.getBytes());

    assertThrows(ClassFormatException.class, () -> classFile.hashCode());
    assertThrows(ClassFormatException.class, () -> classFile.toString());
  }
}
