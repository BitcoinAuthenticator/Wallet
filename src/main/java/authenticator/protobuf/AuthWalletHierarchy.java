// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: AuthWalletHierarchy.proto

package authenticator.protobuf;

public final class AuthWalletHierarchy {
  private AuthWalletHierarchy() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  /**
   * Protobuf enum {@code authenticator.protobuf.HierarchyPurpose}
   */
  public enum HierarchyPurpose
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>Bip43 = 44;</code>
     *
     * <pre>
     * 0x8000002C as in Bip43
     * </pre>
     */
    Bip43(0, 44),
    ;

    /**
     * <code>Bip43 = 44;</code>
     *
     * <pre>
     * 0x8000002C as in Bip43
     * </pre>
     */
    public static final int Bip43_VALUE = 44;


    public final int getNumber() { return value; }

    public static HierarchyPurpose valueOf(int value) {
      switch (value) {
        case 44: return Bip43;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<HierarchyPurpose>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<HierarchyPurpose>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<HierarchyPurpose>() {
            public HierarchyPurpose findValueByNumber(int number) {
              return HierarchyPurpose.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return authenticator.protobuf.AuthWalletHierarchy.getDescriptor().getEnumTypes().get(0);
    }

    private static final HierarchyPurpose[] VALUES = values();

    public static HierarchyPurpose valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private HierarchyPurpose(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:authenticator.protobuf.HierarchyPurpose)
  }

  /**
   * Protobuf enum {@code authenticator.protobuf.HierarchyCoinTypes}
   */
  public enum HierarchyCoinTypes
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>CoinBitcoin = 0;</code>
     *
     * <pre>
     * 0x80000000 as in Bip44
     * </pre>
     */
    CoinBitcoin(0, 0),
    ;

    /**
     * <code>CoinBitcoin = 0;</code>
     *
     * <pre>
     * 0x80000000 as in Bip44
     * </pre>
     */
    public static final int CoinBitcoin_VALUE = 0;


    public final int getNumber() { return value; }

    public static HierarchyCoinTypes valueOf(int value) {
      switch (value) {
        case 0: return CoinBitcoin;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<HierarchyCoinTypes>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<HierarchyCoinTypes>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<HierarchyCoinTypes>() {
            public HierarchyCoinTypes findValueByNumber(int number) {
              return HierarchyCoinTypes.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return authenticator.protobuf.AuthWalletHierarchy.getDescriptor().getEnumTypes().get(1);
    }

    private static final HierarchyCoinTypes[] VALUES = values();

    public static HierarchyCoinTypes valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private HierarchyCoinTypes(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:authenticator.protobuf.HierarchyCoinTypes)
  }

  /**
   * Protobuf enum {@code authenticator.protobuf.HierarchyAddressTypes}
   */
  public enum HierarchyAddressTypes
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>External = 0;</code>
     *
     * <pre>
     * external
     * </pre>
     */
    External(0, 0),
    ;

    /**
     * <code>External = 0;</code>
     *
     * <pre>
     * external
     * </pre>
     */
    public static final int External_VALUE = 0;


    public final int getNumber() { return value; }

    public static HierarchyAddressTypes valueOf(int value) {
      switch (value) {
        case 0: return External;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<HierarchyAddressTypes>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<HierarchyAddressTypes>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<HierarchyAddressTypes>() {
            public HierarchyAddressTypes findValueByNumber(int number) {
              return HierarchyAddressTypes.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return authenticator.protobuf.AuthWalletHierarchy.getDescriptor().getEnumTypes().get(2);
    }

    private static final HierarchyAddressTypes[] VALUES = values();

    public static HierarchyAddressTypes valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private HierarchyAddressTypes(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:authenticator.protobuf.HierarchyAddressTypes)
  }

  /**
   * Protobuf enum {@code authenticator.protobuf.HierarchyPrefixedAccountIndex}
   */
  public enum HierarchyPrefixedAccountIndex
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>PrefixSpending = 0;</code>
     */
    PrefixSpending(0, 0),
    /**
     * <code>PrefixSavings = 1;</code>
     */
    PrefixSavings(1, 1),
    ;

    /**
     * <code>PrefixSpending = 0;</code>
     */
    public static final int PrefixSpending_VALUE = 0;
    /**
     * <code>PrefixSavings = 1;</code>
     */
    public static final int PrefixSavings_VALUE = 1;


    public final int getNumber() { return value; }

    public static HierarchyPrefixedAccountIndex valueOf(int value) {
      switch (value) {
        case 0: return PrefixSpending;
        case 1: return PrefixSavings;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<HierarchyPrefixedAccountIndex>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<HierarchyPrefixedAccountIndex>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<HierarchyPrefixedAccountIndex>() {
            public HierarchyPrefixedAccountIndex findValueByNumber(int number) {
              return HierarchyPrefixedAccountIndex.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return authenticator.protobuf.AuthWalletHierarchy.getDescriptor().getEnumTypes().get(3);
    }

    private static final HierarchyPrefixedAccountIndex[] VALUES = values();

    public static HierarchyPrefixedAccountIndex valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private HierarchyPrefixedAccountIndex(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:authenticator.protobuf.HierarchyPrefixedAccountIndex)
  }


  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\031AuthWalletHierarchy.proto\022\026authenticat" +
      "or.protobuf*\035\n\020HierarchyPurpose\022\t\n\005Bip43" +
      "\020,*%\n\022HierarchyCoinTypes\022\017\n\013CoinBitcoin\020" +
      "\000*%\n\025HierarchyAddressTypes\022\014\n\010External\020\000" +
      "*F\n\035HierarchyPrefixedAccountIndex\022\022\n\016Pre" +
      "fixSpending\020\000\022\021\n\rPrefixSavings\020\001B\025B\023Auth" +
      "WalletHierarchy"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
