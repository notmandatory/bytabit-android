/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.badge.manager;

import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.badge.model.BadgeRequest;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface BadgeServiceApi {

    @PUT("/badges/{id}")
    Single<Badge> put(@Path("id") String id, @Body BadgeRequest badgeRequest);

    @GET("/badges")
    Single<List<Badge>> get(@Query("profilePubKey") String profilePubKey);
}
