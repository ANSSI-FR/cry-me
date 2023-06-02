#!/usr/bin/env python3 
import glob, os, sys, re, shutil, signal, tarfile

### Ctrl-C handler
def handler(signal, frame):
	print("\nSIGINT caught: exiting ...")
	# Remove the cry.me.bundle folder
	try:
		shutil.rmtree("./cry.me.bundle")
	except:
		pass
	sys.exit(0)

VERBOSE = False

if len(sys.argv) < 3:
	print("Error: please provide a folder to bundle as first argument, and an output file as a second argument.")
	sys.exit(-1)

folder = sys.argv[1]
outfile = sys.argv[2]

def remove_comment_from_string(s, fname):
	if "CRY.ME.VULN" in s:
		if VERBOSE is True:
	 		print("[+] Removing CRY.ME.VULN from %s" % fname)
		# Remove the CRY.ME.VULN comment
		inside_comm = False
		found = False
		out = ""
		for l in s.splitlines():
			if "/*" in l:
				inside_comm = True
				curr_comm = ""
			if inside_comm is True:
				curr_comm += l+"\n"
			if "*/" in l:
				inside_comm = False
				if found is False:
					out += curr_comm
				else:
					# Reset
					found = False
				curr_comm = ""
			if "CRY.ME.VULN" in l:
				# We have found the comment
				found = True
			if (inside_comm is False) and ("*/" not in l):
				out += l+"\n"
		return out
	else:
		return s


def remove_comment(f):
	with open(f, "r") as ff:
		s = ff.read()
	s = remove_comment_from_string(s, f)
	with open(f, "w") as ff:
		ff.write(s)

def make_tarfile(output_filename, source_dir):
	with tarfile.open(output_filename, "w:gz") as tar:
		tar.add(source_dir, arcname=os.path.basename(source_dir))

# Register Ctrl+C handler
signal.signal(signal.SIGINT, handler)

# First copy our foler in a temporary space
try:
	shutil.rmtree("./cry.me.bundle")
except:
	pass
shutil.copytree(folder, "./cry.me.bundle")

# File extensions
extensions = [".c", ".h", ".cpp", ".java", ".kt"]
files_to_remove = ["./cry.me.bundle/matrix-sdk-android/matrix-sdk.tar.gz"]
folders_to_remove = ["./cry.me.bundle/matrix-sdk-android/build-tools/", "./cry.me.bundle/matrix-sdk-android/cmake/", "./cry.me.bundle/matrix-sdk-android/emulator/", "./cry.me.bundle/matrix-sdk-android/fonts/", "./cry.me.bundle/matrix-sdk-android/licenses/", "./cry.me.bundle/matrix-sdk-android/ndk/", "./cry.me.bundle/matrix-sdk-android/patcher/", "./cry.me.bundle/matrix-sdk-android/platforms/", "./cry.me.bundle/matrix-sdk-android/platform-tools/", "./cry.me.bundle/matrix-sdk-android/skins/", "./cry.me.bundle/matrix-sdk-android/sources/", "./cry.me.bundle/matrix-sdk-android/system-images/", "./cry.me.bundle/matrix-sdk-android/tools/"]

for root, dirs, files in os.walk("./cry.me.bundle"):
	for file in files:
		path = os.path.join(root, file)
		for e in extensions:
			if file.endswith(e):
				# Remove the possible "CRY.ME.VULN*" comments
				# Add the header
				remove_comment(path)
		for e in files_to_remove:
			if path == e:
				if VERBOSE is True:
					print("[+] Removing file %s" % path)
				# Remove this file from the bundle
				os.remove(path)
		for e in folders_to_remove:
			if e in path:
				# The file is in a folder to remove
				os.remove(path)

# Now that we have removed the vulneraility related stuff, bundle the .tar.gz
make_tarfile(outfile, "./cry.me.bundle")
try:
	shutil.rmtree("./cry.me.bundle")
except:
	pass
