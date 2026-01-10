/*
 * Copyright 2021 devgianlu
 * Copyright [2026] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotifyxp.deps.xyz.gianlu.librespot.api;

import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.EpisodeId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Episode {
    private final ApiClient apiClient;

    protected Episode(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public Metadata.Episode getMetadata(@NotNull EpisodeId episode) throws IOException, MercuryClient.MercuryException {
        ExtendedMetadata.BatchedExtensionResponse response = apiClient.getExtendedMetadata(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4, episode);

        apiClient.checkExtendedMetadataResponse(response);

        return Metadata.Episode.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());
    }
}
