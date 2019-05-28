# TODO: might be simpler to use a bash script.

import subprocess
import sys
import os


# Value must match packageName in build.sbt
IMAGE = "michaelaeden/inquests-ca"

# Directory where build command should be executed.
DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir)


def build(image):
	"""Builds docker image locally using sbt."""
	print("Building image {}".format(image))
	return_code = subprocess.call(["sbt", "docker:publishLocal"], cwd=DIR)
	if return_code != 0:
		print("Failed to build image. Exiting")
		sys.exit(1)


def push(image):
	"""Pushes local docker image to repository."""
	print("Pushing image {}".format(image))
	return_code = subprocess.call(["docker", "push", image], cwd=DIR)
	if return_code != 0:
		print("Failed to push image. Exiting")
		sys.exit(1)


def publish(image):
	"""Builds and publishes docker image."""
	build(image)
	push(image)
	print("Image published successfully!")


if __name__ == "__main__":
	publish(IMAGE)
