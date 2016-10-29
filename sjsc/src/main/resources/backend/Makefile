
# We assume CFLAGS is passed externally, to provide e.g., -g -O3
# We assume TIZEN_SDK is passed as well
#TIZEN_SDK=/Users/colin.gordon/tizen-sdk

INC=-I. -I$(GCBASE)/native/include/
HEADERS=array.h ffi.h globals.h jsmath.h map.h runtime.h xxhash.h
COMMON=array.c globals.c jsmath.c map.c runtime.c xxhash.c date.c interop.c
ASMJS=browser/ffi.c
NATIVE=native/ffi.c

GCBASE=../../../../external/gc

HYGIENE=-Werror=implicit-int -Werror=implicit-function-declaration -Wno-attributes -Wno-unused-value -Wno-parentheses-equality 
#TIZEN_SDK=/Users/colin.gordon/tizen-sdk-2.2/
ARMAR=$(TIZEN_SDK)/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-ar
ARMCC=$(TIZEN_SDK)/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-gcc
#ARMCC=$(TIZEN_SDK)/tools/llvm-3.1/bin/clang
TIZEN_ROOT=$(TIZEN_SDK)/platforms/tizen2.2/rootstraps/tizen-device-2.2.native

GCC=/usr/local/Cellar/gcc/4.9.1/bin/gcc-4.9

EMUAR=$(TIZEN_SDK)/tools/i386-linux-gnueabi-gcc-4.5/bin/i386-linux-gnueabi-ar
EMUCC=$(TIZEN_SDK)/tools/i386-linux-gnueabi-gcc-4.5/bin/i386-linux-gnueabi-gcc
EMULATOR_ROOT=$(TIZEN_SDK)/platforms/tizen2.2/rootstraps/tizen-emulator-2.2.native

TIZEN_FLAGS=-D__SJS__ -DUSE_GC -DTIZEN -lc_nonshared -lm --sysroot $(TIZEN_ROOT) -fPIC -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mtune=cortex-a8 
EMULATOR_FLAGS=-D__SJS__ -DUSE_GC -DTIZEN -lc_nonshared -lm --sysroot $(EMULATOR_ROOT)
NATIVE_FLAGS=-D__SJS__ -DUSE_GC -std=c11 -ftrapv
ASMJS_FLAGS=-D__SJS__ -DLEAK_MEMORY --pre-js browser/marshalling.js --js-library browser/library.js 

NATIVE_GC=$(GCBASE)/boehm_native/extra/gc.o
EMULATOR_GC=$(GCBASE)/boehm_i386/extra/gc.o 
DEVICE_GC=$(GCBASE)/boehm_arm/extra/gc.o 
X86_GC=$(GCBASE)/boehm_32/extra/gc.o

NATIVE_SOURCE=$(COMMON) $(NATIVE)
ASM_SOURCE=$(COMMON) $(ASMJS)


default: native

all: native native-gcc tizen js emulator x86

%.x86.o: %.c $(HEADERS)
	clang -m32 $(CFLAGS) $(HYGIENE) $(INC) $(NATIVE_FLAGS) -c -o $@ $<

%.clang.o: %.c $(HEADERS)
	clang $(CFLAGS) $(HYGIENE) $(INC) $(NATIVE_FLAGS) -c -o $@ $<

%.gcc.o: %.c $(HEADERS)
	$(GCC) $(CFLAGS) $(HYGIENE) $(INC) $(NATIVE_FLAGS) -c -o $@ $<

%.tizen.o: %.c $(HEADERS)
	$(ARMCC) $(TIZEN_FLAGS) $(CFLAGS) $(HYGIENE) $(INC) -c -o $@ $<

%.emulator.o: %.c $(HEADERS)
	$(EMUCC) $(EMULATOR_FLAGS) $(CFLAGS) $(HYGIENE) $(INC) -c -o $@ $<

sjs_runtime.x86.a: $(NATIVE_SOURCE:%.c=%.x86.o)
	ar rcsv $@ $(NATIVE_SOURCE:%.c=%.x86.o) $(X86_GC)

sjs_runtime.clang.a: $(NATIVE_SOURCE:%.c=%.clang.o)
	ar rcsv $@ $(NATIVE_SOURCE:%.c=%.clang.o) $(NATIVE_GC)

sjs_runtime.gcc.a: $(NATIVE_SOURCE:%.c=%.gcc.o)
	ar rcsv $@ $(NATIVE_SOURCE:%.c=%.gcc.o) $(NATIVE_GC)

sjs_runtime.tizen.a: $(NATIVE_SOURCE:%.c=%.tizen.o)
	$(ARMAR) rcsv $@ $(NATIVE_SOURCE:%.c=%.tizen.o)	$(DEVICE_GC)

sjs_runtime.emulator.a: $(NATIVE_SOURCE:%.c=%.emulator.o)
	$(EMUAR) rcsv $@ $(NATIVE_SOURCE:%.c=%.emulator.o) $(EMULATOR_GC)

native: sjs_runtime.clang.a

native-gcc: sjs_runtime.gcc.a

tizen: sjs_runtime.tizen.a

js: sjs_runtime.emcc.a

emulator: sjs_runtime.emulator.a

x86: sjs_runtime.x86.a

%.emcc.o: %.c $(HEADERS)
	emcc $(CFLAGS) $(HYGIENE) $(INC) $(ASMJS_FLAGS) -c -o $@ $<

sjs_runtime.emcc.a: $(ASM_SOURCE:%.c=%.emcc.o)
	emar rcs $@ $(ASM_SOURCE:%.c=%.emcc.o)

clean:
	rm -f *.o *.a
