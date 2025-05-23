/*
	https://github.com/DmitriiShamrikov/mslinks
	
	Copyright (c) 2022 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package com.spotifyxp.deps.mslinks.data;

import com.spotifyxp.deps.io.ByteReader;
import com.spotifyxp.deps.io.ByteWriter;
import com.spotifyxp.deps.mslinks.ShellLinkException;

import java.io.IOException;

public class ItemIDUnknown extends ItemID {

    protected byte[] data;

    public ItemIDUnknown(int flags) {
        super(flags);
    }

    @Override
    public void load(ByteReader br, int maxSize) throws IOException, ShellLinkException {
        int startPos = br.getPosition();

        super.load(br, maxSize);

        int bytesRead = br.getPosition() - startPos;
        data = new byte[maxSize - bytesRead];
        br.read(data);
    }

    @Override
    public void serialize(ByteWriter bw) throws IOException {
        super.serialize(bw);
        bw.write(data);
    }

    @Override
    public String toString() {
        return String.format("<ItemIDUnknown 0x%02X>", typeFlags);
    }
}
