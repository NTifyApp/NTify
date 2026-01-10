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
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.ShowId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Show {
    private final ApiClient apiClient;

    protected Show(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public Metadata.Show getMetadata(@NotNull ShowId show) throws IOException, MercuryClient.MercuryException {
        ExtendedMetadata.BatchedExtensionResponse response = apiClient.getExtendedMetadata(ExtensionKindOuterClass.ExtensionKind.SHOW_V4, show);

        apiClient.checkExtendedMetadataResponse(response);

        return Metadata.Show.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());
    }

    public void follow(ShowId showId) throws IOException, MercuryClient.MercuryException {
        apiClient.unfollow(showId);
    }

    public void unfollow(ShowId showId) throws IOException, MercuryClient.MercuryException {
        apiClient.unfollow(showId);
    }
}
