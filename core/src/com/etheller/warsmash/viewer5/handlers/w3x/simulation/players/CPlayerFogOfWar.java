package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import java.nio.ByteBuffer;

import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;

public class CPlayerFogOfWar {
	private final int width;
	private final int height;
	private final ByteBuffer fogOfWarBuffer;

	public CPlayerFogOfWar(final PathingGrid pathingGrid) {
		width = (pathingGrid.getWidth() / 8) + 1;
		height = (pathingGrid.getHeight() / 8) + 1;
		final int fogOfWarBufferLen = width * height;
		this.fogOfWarBuffer = ByteBuffer.allocateDirect(fogOfWarBufferLen);
		fogOfWarBuffer.clear();
		while (fogOfWarBuffer.hasRemaining()) {
			fogOfWarBuffer.put((byte) -1);
		}
		fogOfWarBuffer.clear();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public ByteBuffer getFogOfWarBuffer() {
		return fogOfWarBuffer;
	}

	public byte getState(final PathingGrid pathingGrid, final float x, final float y) {
		final int indexX = pathingGrid.getFogOfWarIndexX(x);
		final int indexY = pathingGrid.getFogOfWarIndexY(y);
		return getState(indexX, indexY);
	}

	public byte getState(final int indexX, final int indexY) {
		final int index = (indexY * getWidth()) + indexX;
		if ((index >= 0) && (index < fogOfWarBuffer.capacity())) {
			return fogOfWarBuffer.get(index);
		}
		return 0;
	}

	public void setState(final PathingGrid pathingGrid, final float x, final float y, final byte fogOfWarState) {
		final int indexX = pathingGrid.getFogOfWarIndexX(x);
		final int indexY = pathingGrid.getFogOfWarIndexY(y);
		setState(indexX, indexY, fogOfWarState);
	}

	public void setState(final int indexX, final int indexY, final byte fogOfWarState) {
		final int writeIndex = (indexY * getWidth()) + indexX;
		if ((writeIndex >= 0) && (writeIndex < fogOfWarBuffer.capacity())) {
			fogOfWarBuffer.put(writeIndex, fogOfWarState);
		}
	}

	public void convertVisibleToFogged() {
		for (int i = 0; i < fogOfWarBuffer.capacity(); i++) {
			if (fogOfWarBuffer.get(i) == 0) {
				fogOfWarBuffer.put(i, (byte) 127);
			}
		}
	}
}
