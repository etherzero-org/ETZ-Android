# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

# now build app's shared lib
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99 -DANDROID_STL=gnustl_static -DANDROID_TOOLCHAIN=clang")

# -Wimplicit-function-declaration
# -Wno-missing-prototypes -Werror=return-type -Wdocumentation -Wunreachable-code-aggressive -Wno-missing-braces
# -Wparentheses -Wswitch -Wno-unused-function -Wunused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wempty-body
# -Wconditional-uninitialized -Wno-unknown-pragmas -pedantic -Wshadow -Wfour-char-constants -Wno-conversion -Wconstant-conversion
# -Wint-conversion -Wbool-conversion -Wenum-conversion -Wassign-enum -Wno-shorten-64-to-32 -Wpointer-sign -Wnewline-eof
# -Wdeprecated-declarations -Wno-sign-conversion

add_library( # The name of the library.
             core

             # Shared library.
             SHARED

             # Core files
             src/main/jni/etzwallet-core/BRAddress.c
             src/main/jni/etzwallet-core/BRAddress.h
             src/main/jni/etzwallet-core/BRArray.h
             src/main/jni/etzwallet-core/BRBase58.c
             src/main/jni/etzwallet-core/BRBase58.h
             src/main/jni/etzwallet-core/BRBech32.c
             src/main/jni/etzwallet-core/BRBech32.h
             src/main/jni/etzwallet-core/BRBIP32Sequence.c
             src/main/jni/etzwallet-core/BRBIP32Sequence.h
             src/main/jni/etzwallet-core/BRBIP38Key.c
             src/main/jni/etzwallet-core/BRBIP38Key.h
             src/main/jni/etzwallet-core/BRBIP39Mnemonic.c
             src/main/jni/etzwallet-core/BRBIP39Mnemonic.h
             src/main/jni/etzwallet-core/BRBIP39WordsEn.h
             src/main/jni/etzwallet-core/BRBloomFilter.c
             src/main/jni/etzwallet-core/BRBloomFilter.h
             src/main/jni/etzwallet-core/BRCrypto.c
             src/main/jni/etzwallet-core/BRCrypto.h
             src/main/jni/etzwallet-core/BRInt.h
             src/main/jni/etzwallet-core/BRKey.c
             src/main/jni/etzwallet-core/BRKey.h
             src/main/jni/etzwallet-core/BRKeyECIES.c
             src/main/jni/etzwallet-core/BRKeyECIES.h
             src/main/jni/etzwallet-core/BRMerkleBlock.c
             src/main/jni/etzwallet-core/BRMerkleBlock.h
             src/main/jni/etzwallet-core/BRPaymentProtocol.c
             src/main/jni/etzwallet-core/BRPaymentProtocol.h
             src/main/jni/etzwallet-core/BRPeer.c
             src/main/jni/etzwallet-core/BRPeer.h
             src/main/jni/etzwallet-core/BRPeerManager.c
             src/main/jni/etzwallet-core/BRPeerManager.h
             src/main/jni/etzwallet-core/BRSet.c
             src/main/jni/etzwallet-core/BRSet.h
             src/main/jni/etzwallet-core/BRTransaction.c
             src/main/jni/etzwallet-core/BRTransaction.h
             src/main/jni/etzwallet-core/BRWallet.c
             src/main/jni/etzwallet-core/BRWallet.h

             # BCash files
             src/main/jni/etzwallet-core/bcash/BRBCashAddr.c
             src/main/jni/etzwallet-core/bcash/BRBCashAddr.h
             src/main/jni/etzwallet-core/bcash/BRBCashParams.h

             # Core JNI files
             src/main/jni/etzwallet-core/Java/BRCoreJni.h
             src/main/jni/etzwallet-core/Java/BRCoreJni.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreJniReference.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreJniReference.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreAddress.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreAddress.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreChainParams.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreChainParams.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePeer.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePeer.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreMasterPubKey.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreMasterPubKey.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreKey.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreKey.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreMerkleBlock.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreMerkleBlock.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePeerManager.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePeerManager.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransaction.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransaction.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransactionInput.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransactionInput.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransactionOutput.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreTransactionOutput.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePaymentProtocol.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCorePaymentProtocol.h
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreWallet.c
             src/main/jni/etzwallet-core/Java/com_etzwallet_core_BRCoreWallet.h

             # Bech32 files
             src/main/jni/etzwallet-core/BRBech32.c
             src/main/jni/etzwallet-core/BRBech32.h

             # Ethereum files
             src/main/jni/etzwallet-core/ethereum/BREthereum.h
             src/main/jni/etzwallet-core/ethereum/BREthereum.c
             src/main/jni/etzwallet-core/ethereum/BREthereumAccount.c
             src/main/jni/etzwallet-core/ethereum/BREthereumAccount.h
             src/main/jni/etzwallet-core/ethereum/BREthereumAccountState.c
             src/main/jni/etzwallet-core/ethereum/BREthereumAccountState.h
             src/main/jni/etzwallet-core/ethereum/BREthereumAmount.c
             src/main/jni/etzwallet-core/ethereum/BREthereumAmount.h
             src/main/jni/etzwallet-core/ethereum/BREthereumBase.h
             src/main/jni/etzwallet-core/ethereum/BREthereumBlock.c
             src/main/jni/etzwallet-core/ethereum/BREthereumBlock.h
             src/main/jni/etzwallet-core/ethereum/BREthereumBloomFilter.c
             src/main/jni/etzwallet-core/ethereum/BREthereumBloomFilter.h
             src/main/jni/etzwallet-core/ethereum/BREthereumContract.c
             src/main/jni/etzwallet-core/ethereum/BREthereumContract.h
             src/main/jni/etzwallet-core/ethereum/BREthereumEther.c
             src/main/jni/etzwallet-core/ethereum/BREthereumEther.h
             src/main/jni/etzwallet-core/ethereum/BREthereumGas.c
             src/main/jni/etzwallet-core/ethereum/BREthereumGas.h
             src/main/jni/etzwallet-core/ethereum/BREthereumHash.c
             src/main/jni/etzwallet-core/ethereum/BREthereumLightNode.c
             src/main/jni/etzwallet-core/ethereum/BREthereumLightNode.h
             src/main/jni/etzwallet-core/ethereum/BREthereumLightNodeAnnounce.c
             src/main/jni/etzwallet-core/ethereum/BREthereumLightNodeListen.c
             src/main/jni/etzwallet-core/ethereum/BREthereumLightNodePrivate.h
             src/main/jni/etzwallet-core/ethereum/BREthereumLog.c
             src/main/jni/etzwallet-core/ethereum/BREthereumLog.h
             src/main/jni/etzwallet-core/ethereum/BREthereumNetwork.c
             src/main/jni/etzwallet-core/ethereum/BREthereumNetwork.h
             src/main/jni/etzwallet-core/ethereum/BREthereumPrivate.h
             src/main/jni/etzwallet-core/ethereum/BREthereumToken.c
             src/main/jni/etzwallet-core/ethereum/BREthereumToken.h
             src/main/jni/etzwallet-core/ethereum/BREthereumTransaction.c
             src/main/jni/etzwallet-core/ethereum/BREthereumTransaction.h
             src/main/jni/etzwallet-core/ethereum/BREthereumTransactionReceipt.c
             src/main/jni/etzwallet-core/ethereum/BREthereumTransactionReceipt.h
             src/main/jni/etzwallet-core/ethereum/BREthereumWallet.c
             src/main/jni/etzwallet-core/ethereum/BREthereumWallet.h

             # Ethereum Event
             src/main/jni/etzwallet-core/ethereum/event/BREvent.c
             src/main/jni/etzwallet-core/ethereum/event/BREvent.h
             src/main/jni/etzwallet-core/ethereum/event/BREventQueue.c
             src/main/jni/etzwallet-core/ethereum/event/BREventQueue.h

             #Ethereum RLP
             src/main/jni/etzwallet-core/ethereum/rlp/BRRlp.h
             src/main/jni/etzwallet-core/ethereum/rlp/BRRlpCoder.c
             src/main/jni/etzwallet-core/ethereum/rlp/BRRlpCoder.h

             #Ethereum Util
             src/main/jni/etzwallet-core/ethereum/util/BRUtil.h
             src/main/jni/etzwallet-core/ethereum/util/BRUtilHex.c
             src/main/jni/etzwallet-core/ethereum/util/BRUtilHex.h
             src/main/jni/etzwallet-core/ethereum/util/BRUtilMath.c
             src/main/jni/etzwallet-core/ethereum/util/BRUtilMath.h
             src/main/jni/etzwallet-core/ethereum/util/BRUtilMathParse.c

             #Ethereum JNI files
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumAccount.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumAmount.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumLightNode.c
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumLightNode.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumNetwork.c
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumNetwork.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumToken.c
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumToken.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumTransaction.h
             src/main/jni/etzwallet-core/ethereum/Java/com_etzwallet_core_ethereum_BREthereumWallet.h

             )

include_directories(src/main/jni/etzwallet-core/ethereum/rlp/)
include_directories(src/main/jni/etzwallet-core/ethereum/util/)
include_directories(src/main/jni/etzwallet-core/ethereum/Java/)
include_directories(src/main/jni/etzwallet-core/ethereum/)
include_directories(src/main/jni/etzwallet-core/bcash/)
include_directories(src/main/jni/etzwallet-core/Java/)
include_directories(src/main/jni/etzwallet-core/)
include_directories(src/main/jni/etzwallet-core/secp256k1/include/)
include_directories(src/main/jni/etzwallet-core/secp256k1/src/)
include_directories(src/main/jni/etzwallet-core/secp256k1/)

include_directories(src/main/secp/secp256k1/include/)
include_directories(src/main/secp/secp256k1/src/)
include_directories(src/main/secp/secp256k1/)
include_directories(src/main/secp/)

# add lib dependencies
target_link_libraries(
    core
    android
    log)
