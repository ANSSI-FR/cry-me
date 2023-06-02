#/*************************** The CRY.ME project (2023) *************************************************
# *
# *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
# *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
# *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
# *  Please do not use this source code outside this scope, or use it knowingly.
# *
# *  Many files come from the Android element (https://github.com/vector-im/element-android), the
# *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
# *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
# *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
# *  under the Apache-2.0 license, and so is the CRY.ME project.
# *
# ***************************  (END OF CRY.ME HEADER)   *************************************************/
#
# Copyright 2014-2016 OpenMarket Ltd
# Copyright 2020-2021 The Matrix.org Foundation C.I.C.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import logging
from io import BytesIO
from typing import Tuple

from PIL import Image

logger = logging.getLogger(__name__)

EXIF_ORIENTATION_TAG = 0x0112
EXIF_TRANSPOSE_MAPPINGS = {
    2: Image.FLIP_LEFT_RIGHT,
    3: Image.ROTATE_180,
    4: Image.FLIP_TOP_BOTTOM,
    5: Image.TRANSPOSE,
    6: Image.ROTATE_270,
    7: Image.TRANSVERSE,
    8: Image.ROTATE_90,
}


class ThumbnailError(Exception):
    """An error occurred generating a thumbnail."""


class Thumbnailer:

    FORMATS = {"image/jpeg": "JPEG", "image/png": "PNG"}

    @staticmethod
    def set_limits(max_image_pixels: int) -> None:
        Image.MAX_IMAGE_PIXELS = max_image_pixels

    def __init__(self, input_path: str):
        try:
            self.image = Image.open(input_path)
        except OSError as e:
            # If an error occurs opening the image, a thumbnail won't be able to
            # be generated.
            raise ThumbnailError from e
        except Image.DecompressionBombError as e:
            # If an image decompression bomb error occurs opening the image,
            # then the image exceeds the pixel limit and a thumbnail won't
            # be able to be generated.
            raise ThumbnailError from e

        self.width, self.height = self.image.size
        self.transpose_method = None
        try:
            # We don't use ImageOps.exif_transpose since it crashes with big EXIF
            #
            # Ignore safety: Pillow seems to acknowledge that this method is
            # "private, experimental, but generally widely used". Pillow 6
            # includes a public getexif() method (no underscore) that we might
            # consider using instead when we can bump that dependency.
            #
            # At the time of writing, Debian buster (currently oldstable)
            # provides version 5.4.1. It's expected to EOL in mid-2022, see
            # https://wiki.debian.org/DebianReleases#Production_Releases
            image_exif = self.image._getexif()  # type: ignore
            if image_exif is not None:
                image_orientation = image_exif.get(EXIF_ORIENTATION_TAG)
                assert isinstance(image_orientation, int)
                self.transpose_method = EXIF_TRANSPOSE_MAPPINGS.get(image_orientation)
        except Exception as e:
            # A lot of parsing errors can happen when parsing EXIF
            logger.info("Error parsing image EXIF information: %s", e)

    def transpose(self) -> Tuple[int, int]:
        """Transpose the image using its EXIF Orientation tag

        Returns:
            A tuple containing the new image size in pixels as (width, height).
        """
        if self.transpose_method is not None:
            # Safety: `transpose` takes an int rather than e.g. an IntEnum.
            # self.transpose_method is set above to be a value in
            # EXIF_TRANSPOSE_MAPPINGS, and that only contains correct values.
            self.image = self.image.transpose(self.transpose_method)  # type: ignore[arg-type]
            self.width, self.height = self.image.size
            self.transpose_method = None
            # We don't need EXIF any more
            self.image.info["exif"] = None
        return self.image.size

    def aspect(self, max_width: int, max_height: int) -> Tuple[int, int]:
        """Calculate the largest size that preserves aspect ratio which
        fits within the given rectangle::

            (w_in / h_in) = (w_out / h_out)
            w_out = max(min(w_max, h_max * (w_in / h_in)), 1)
            h_out = max(min(h_max, w_max * (h_in / w_in)), 1)

        Args:
            max_width: The largest possible width.
            max_height: The largest possible height.
        """

        if max_width * self.height < max_height * self.width:
            return max_width, max((max_width * self.height) // self.width, 1)
        else:
            return max((max_height * self.width) // self.height, 1), max_height

    def _resize(self, width: int, height: int) -> Image.Image:
        # 1-bit or 8-bit color palette images need converting to RGB
        # otherwise they will be scaled using nearest neighbour which
        # looks awful.
        #
        # If the image has transparency, use RGBA instead.
        if self.image.mode in ["1", "L", "P"]:
            mode = "RGB"
            if self.image.info.get("transparency", None) is not None:
                mode = "RGBA"
            self.image = self.image.convert(mode)
        return self.image.resize((width, height), Image.ANTIALIAS)

    def scale(self, width: int, height: int, output_type: str) -> BytesIO:
        """Rescales the image to the given dimensions.

        Returns:
            BytesIO: the bytes of the encoded image ready to be written to disk
        """
        scaled = self._resize(width, height)
        return self._encode_image(scaled, output_type)

    def crop(self, width: int, height: int, output_type: str) -> BytesIO:
        """Rescales and crops the image to the given dimensions preserving
        aspect::
            (w_in / h_in) = (w_scaled / h_scaled)
            w_scaled = max(w_out, h_out * (w_in / h_in))
            h_scaled = max(h_out, w_out * (h_in / w_in))

        Args:
            max_width: The largest possible width.
            max_height: The largest possible height.

        Returns:
            BytesIO: the bytes of the encoded image ready to be written to disk
        """
        if width * self.height > height * self.width:
            scaled_height = (width * self.height) // self.width
            scaled_image = self._resize(width, scaled_height)
            crop_top = (scaled_height - height) // 2
            crop_bottom = height + crop_top
            cropped = scaled_image.crop((0, crop_top, width, crop_bottom))
        else:
            scaled_width = (height * self.width) // self.height
            scaled_image = self._resize(scaled_width, height)
            crop_left = (scaled_width - width) // 2
            crop_right = width + crop_left
            cropped = scaled_image.crop((crop_left, 0, crop_right, height))
        return self._encode_image(cropped, output_type)

    def _encode_image(self, output_image: Image.Image, output_type: str) -> BytesIO:
        output_bytes_io = BytesIO()
        fmt = self.FORMATS[output_type]
        if fmt == "JPEG":
            output_image = output_image.convert("RGB")
        output_image.save(output_bytes_io, fmt, quality=80)
        return output_bytes_io
