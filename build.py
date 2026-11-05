import os
import sys
import subprocess
import shutil

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_JAVA_DIR = os.path.join(PROJECT_DIR, "src", "main", "java")
SRC_RES_DIR = os.path.join(PROJECT_DIR, "src", "main", "resources")
LIB_DIR = os.path.join(PROJECT_DIR, "lib")
TARGET_DIR = os.path.join(PROJECT_DIR, "target")
CLASSES_DIR = os.path.join(TARGET_DIR, "classes")
OUTPUT_JAR = os.path.join(TARGET_DIR, "MineCubes-2.1.0.jar")

JDK_BIN = r"C:\Program Files\Java\jdk-26.0.2.1\bin"
JAVAC = os.path.join(JDK_BIN, "javac.exe")
JAR = os.path.join(JDK_BIN, "jar.exe")

def build():
    print("========================================")
    print("       Building MineCubes v2.1.0        ")
    print("   Target: Minecraft 26.2 (DivineMC/Paper)")
    print("========================================")

    if os.path.exists(CLASSES_DIR):
        shutil.rmtree(CLASSES_DIR)
    os.makedirs(CLASSES_DIR, exist_ok=True)
    os.makedirs(TARGET_DIR, exist_ok=True)

    # 1. Collect libraries from lib/
    lib_jars = []
    if os.path.exists(LIB_DIR):
        for f in os.listdir(LIB_DIR):
            if f.endswith(".jar"):
                lib_jars.append(os.path.join(LIB_DIR, f).replace("\\", "/"))
    print(f"Found {len(lib_jars)} library jars in {LIB_DIR}")

    # 2. Collect Java sources
    java_files = []
    for root, dirs, files in os.walk(SRC_JAVA_DIR):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f).replace("\\", "/"))
    print(f"Found {len(java_files)} Java source files")

    # 3. Create javac args file
    classes_dir_arg = CLASSES_DIR.replace("\\", "/")
    cp_str = ";".join(lib_jars)
    args_file = os.path.join(PROJECT_DIR, "javac_args.txt")
    with open(args_file, "w", encoding="utf-8") as f:
        f.write("--release 21\n")
        f.write(f"-d \"{classes_dir_arg}\"\n")
        f.write("-encoding UTF-8\n")
        f.write(f"-cp \"{cp_str}\"\n")
        for jf in java_files:
            f.write(f"\"{jf}\"\n")

    # 4. Compile with javac
    print("Compiling Java classes with javac...")
    result = subprocess.run([JAVAC, f"@{args_file}"], capture_output=True, text=True)
    if result.returncode != 0:
        print("COMPILATION FAILED:")
        print(result.stdout)
        print(result.stderr)
        sys.exit(1)
    else:
        print("Compilation successful!")
        if result.stderr:
            print(result.stderr.strip())

    # 5. Copy resources
    print("Copying resources...")
    for root, dirs, files in os.walk(SRC_RES_DIR):
        rel = os.path.relpath(root, SRC_RES_DIR)
        dest_dir = os.path.join(CLASSES_DIR, rel) if rel != "." else CLASSES_DIR
        os.makedirs(dest_dir, exist_ok=True)
        for f in files:
            src_f = os.path.join(root, f)
            dest_f = os.path.join(dest_dir, f)
            shutil.copy2(src_f, dest_f)
            print(f" Copied resource: {f}")

    # 6. Package JAR
    print("Packaging JAR...")
    jar_cmd = [JAR, "cf", OUTPUT_JAR, "-C", CLASSES_DIR, "."]
    result = subprocess.run(jar_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("JAR PACKAGING FAILED:")
        print(result.stdout)
        print(result.stderr)
        sys.exit(1)
    print(f"Created JAR: {OUTPUT_JAR} ({os.path.getsize(OUTPUT_JAR)} bytes)")

    # 7. Verify JAR contents
    result = subprocess.run([JAR, "tf", OUTPUT_JAR], capture_output=True, text=True)
    lines = result.stdout.strip().splitlines()
    classes = [l for l in lines if l.endswith(".class")]
    resources = [l for l in lines if not l.endswith(".class") and not l.endswith("/")]
    print(f"\nJAR Verified successfully: {len(classes)} classes, {len(resources)} resource files.")
    for r in resources:
        print(f"   Resource: {r}")

    print("\n>>> BUILD COMPLETE: CubeGen v2.0.0 is ready for deployment! <<<")

if __name__ == "__main__":
    build()
