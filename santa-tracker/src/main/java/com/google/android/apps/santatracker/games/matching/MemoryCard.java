/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.games.matching;

import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Card in the memory game.
 * Contains the front of the card (the card image) and its back (its cloak).
 */
public class MemoryCard {

    public int mCardImageId;
    public int mCardCloakId;
    public View mView;

    public MemoryCard(int cardImageId, int cardCloakId) {
        mCardImageId = cardImageId;
        mCardCloakId = cardCloakId;
    }

    /**
     * Generate a randomised list of {@link com.google.android.apps.santatracker.games.matching.MemoryCard}s.
     *
     * @param numCards   Number of cards to generate
     * @param cardImages List of card image references
     * @param cardCloaks List of card cloak image references
     */
    public static ArrayList<MemoryCard> getGameCards(int numCards, List<Integer> cardImages,
            List<Integer> cardCloaks) {
        Collections.shuffle(cardImages);
        Collections.shuffle(cardCloaks);
        ArrayList<MemoryCard> cards = new ArrayList<MemoryCard>();
        for (int i = 0; i < (numCards / 2); i++) {
            cards.add(new MemoryCard(cardImages.get(i), cardCloaks.get(i)));
            cards.add(new MemoryCard(cardImages.get(i), cardCloaks.get(i)));
        }
        Collections.shuffle(cards);
        return cards;
    }
}
