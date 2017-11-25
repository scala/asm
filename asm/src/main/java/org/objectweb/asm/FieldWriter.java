// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm;

/**
 * A {@link FieldVisitor} that generates a corresponding 'field_info' structure, as defined in the
 * Java Virtual Machine Specification (JVMS).
 *
 * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5
 * @author Eric Bruneton
 */
final class FieldWriter extends FieldVisitor {

  /** Where the constants used in this FieldWriter must be stored. */
  private final SymbolTable symbolTable;

  /** The access_flags field of the field_info JVMS structure. */
  private final int accessFlags;

  /** The name_index field of the field_info JVMS structure. */
  private final int nameIndex;

  /** The descriptor_index field of the field_info JVMS structure. */
  private final int descriptorIndex;

  /**
   * The signature_index field of the Signature attribute of this field_info, or 0 if there is no
   * Signature attribute.
   */
  private int signatureIndex;

  /**
   * The constantvalue_index field of the ConstantValue attribute of this field_info, or 0 if there
   * is no ConstantValue attribute.
   */
  private int constantValueIndex;

  /**
   * The last runtime visible annotation of this field. The previous ones can be accessed with the
   * {@link AnnotationWriter#previousAnnotation} field. May be <tt>null</tt>.
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * The last runtime invisible annotation of this field. The previous ones can be accessed with the
   * {@link AnnotationWriter#previousAnnotation} field. May be <tt>null</tt>.
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * The last runtime visible type annotation of this field. The previous ones can be accessed with
   * the {@link AnnotationWriter#previousAnnotation} field. May be <tt>null</tt>.
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * The last runtime invisible type annotation of this field. The previous ones can be accessed
   * with the {@link AnnotationWriter#previousAnnotation} field. May be <tt>null</tt>.
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * The first non standard attribute of this field. The next ones can be accessed with the {@link
   * Attribute#nextAttribute} field. May be <tt>null</tt>.
   *
   * <p><b>WARNING</b>: this list stores the attributes in the <i>reverse</i> order of their visit.
   * firstAttribute is actually the last attribute visited in {@link #visitAttribute()}. The {@link
   * #put()} method writes the attributes in the order defined by this list, i.e. in the reverse
   * order specified by the user.
   */
  private Attribute firstAttribute;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------

  /**
   * Constructs a new {@link FieldWriter}.
   *
   * @param symbolTable where the constants used in this FieldWriter must be stored.
   * @param access the field's access flags (see {@link Opcodes}).
   * @param name the field's name.
   * @param descriptor the field's descriptor (see {@link Type}).
   * @param signature the field's signature. May be <tt>null</tt>.
   * @param constantValue the field's constant value. May be <tt>null</tt>.
   */
  FieldWriter(
      final SymbolTable symbolTable,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object constantValue) {
    super(Opcodes.ASM6);
    this.symbolTable = symbolTable;
    this.accessFlags = access;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
    if (constantValue != null) {
      this.constantValueIndex = symbolTable.addConstant(constantValue).index;
    }
  }

  // ------------------------------------------------------------------------
  // Implementation of the FieldVisitor abstract class
  // ------------------------------------------------------------------------

  @Override
  public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
    // Create a ByteVector to hold an 'annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.
    ByteVector annotation = new ByteVector();
    // Write type_index and reserve space for num_element_value_pairs.
    annotation.putShort(symbolTable.addConstantUtf8(desc)).putShort(0);
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          new AnnotationWriter(symbolTable, annotation, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          new AnnotationWriter(symbolTable, annotation, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String desc, final boolean visible) {
    // Create a ByteVector to hold a 'type_annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
    ByteVector typeAnnotation = new ByteVector();
    // Write target_type, target_info, and target_path.
    TypeReference.putTarget(typeRef, typeAnnotation);
    TypePath.put(typePath, typeAnnotation);
    // Write type_index and reserve space for num_element_value_pairs.
    typeAnnotation.putShort(symbolTable.addConstantUtf8(desc)).putShort(0);
    if (visible) {
      return lastRuntimeVisibleTypeAnnotation =
          new AnnotationWriter(symbolTable, typeAnnotation, lastRuntimeVisibleTypeAnnotation);
    } else {
      return lastRuntimeInvisibleTypeAnnotation =
          new AnnotationWriter(symbolTable, typeAnnotation, lastRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {}

  // ------------------------------------------------------------------------
  // Utility methods
  // ------------------------------------------------------------------------

  /**
   * Returns the size of the field_info JVMS structure generated by this FieldWriter. Also add the
   * names of the attributes of this field in the constant pool.
   *
   * @return the size in bytes of the field_info JVMS structure.
   */
  int computeFieldInfoSize() {
    // The access_flags, name_index, descriptor_index and attributes_count fields use 8 bytes.
    int size = 8;
    if (constantValueIndex != 0) {
      // ConstantValue attributes always use 8 bytes.
      symbolTable.addConstantUtf8("ConstantValue");
      size += 8;
    }
    // Before Java 1.5, synthetic fields are represented with a Synthetic attribute.
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && symbolTable.majorVersion() < Opcodes.V1_5) {
      // Synthetic attributes always use 6 bytes.
      symbolTable.addConstantUtf8("Synthetic");
      size += 6;
    }
    // ACC_DEPRECATED is ASM specific, the ClassFile format uses a Deprecated attribute instead.
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      // Deprecated attributes always use 6 bytes.
      symbolTable.addConstantUtf8("Deprecated");
      size += 6;
    }
    if (signatureIndex != 0) {
      // Signature attributes always use 8 bytes.
      symbolTable.addConstantUtf8("Signature");
      size += 8;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      size += lastRuntimeVisibleAnnotation.computeAnnotationsSize("RuntimeVisibleAnnotations");
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      size += lastRuntimeInvisibleAnnotation.computeAnnotationsSize("RuntimeInvisibleAnnotations");
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize("RuntimeVisibleTypeAnnotations");
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              "RuntimeInvisibleTypeAnnotations");
    }
    if (firstAttribute != null) {
      size += firstAttribute.getAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * Puts the content of the field_info JVMS structure generated by this FieldWriter into the given
   * ByteVector.
   *
   * @param output where the field_info structure must be put.
   */
  void put(final ByteVector output) {
    boolean useSyntheticAttribute = symbolTable.majorVersion() < Opcodes.V1_5;
    // Put the access_flags, name_index and descriptor_index fields.
    int mask = Opcodes.ACC_DEPRECATED | (useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0);
    output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
    // Compute and put the attributes_count field.
    int attributesCount = 0;
    if (constantValueIndex != 0) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributesCount;
    }
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
    }
    output.putShort(attributesCount);
    // Put the field_info attributes.
    if (constantValueIndex != 0) {
      output.putShort(symbolTable.addConstantUtf8("ConstantValue"));
      output.putInt(2).putShort(constantValueIndex);
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      output.putShort(symbolTable.addConstantUtf8("Synthetic")).putInt(0);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      output.putShort(symbolTable.addConstantUtf8("Deprecated")).putInt(0);
    }
    if (signatureIndex != 0) {
      output.putShort(symbolTable.addConstantUtf8("Signature"));
      output.putInt(2).putShort(signatureIndex);
    }
    if (lastRuntimeVisibleAnnotation != null) {
      lastRuntimeVisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8("RuntimeVisibleAnnotations"), output);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      lastRuntimeInvisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8("RuntimeInvisibleAnnotations"), output);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      lastRuntimeVisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8("RuntimeVisibleTypeAnnotations"), output);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      lastRuntimeInvisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8("RuntimeInvisibleTypeAnnotations"), output);
    }
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }
}