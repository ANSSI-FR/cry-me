#! /usr/bin/env python3

from smartcard.scard import *
from smartcard.pcsc.PCSCExceptions import *
from smartcard.util import toHexString, toASCIIString
from struct import unpack

hresult, hcontext = SCardEstablishContext(SCARD_SCOPE_USER)
if hresult != SCARD_S_SUCCESS:
	raise EstablishContextException(hresult)

hresult, readers = SCardListReaders(hcontext, [])
if hresult != SCARD_S_SUCCESS:
	raise ListReadersException(hresult)

for reader in readers:
	if "YubiKey" in reader:
		hresult, hcard, dwActiveProtocol = SCardConnect(hcontext, reader,
			SCARD_SHARE_DIRECT, SCARD_PROTOCOL_ANY)
		if hresult != SCARD_S_SUCCESS:
			raise BaseSCardException(hresult)
		# Get the USB bus
		hresult, attrib = SCardGetAttrib(hcard, SCARD_ATTR_CHANNEL_ID)
		# get the DWORD value
		DDDDCCCC = unpack("i", bytearray(attrib))[0]
		DDDD = DDDDCCCC >> 16
		if DDDD == 0x0020:
			bus = (DDDDCCCC & 0xFF00) >> 8
			addr = DDDDCCCC & 0xFF
			print("Bus %03d Device %03d:" % (bus, addr))
		hresult = SCardDisconnect(hcard, SCARD_LEAVE_CARD)
		if hresult != SCARD_S_SUCCESS:
			raise BaseSCardException(hresult)

hresult = SCardReleaseContext(hcontext)
if hresult != SCARD_S_SUCCESS:
	raise ReleaseContextException(hresult)
