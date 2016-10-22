

To build the IDL example, from the sjsc/ directory:

    ./parse_sjs_idl.py idl/tizen idl/tizen.idl
    ./sjsc --native-libs idl/tizen.linkage.json --extra-decls idl/tizen.json --extra-objs idl/tizen.cpp --c-compiler `pwd`/tizen-gcc.sh idl/tizenstr.js 

You may need to comment out the -ferror-limit=100 and -Werror=string-plus-int (not supported by GCC)
and update the paths in tizen-gcc.sh for your machine.
    
