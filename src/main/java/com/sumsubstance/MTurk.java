package com.sumsubstance;

import java.util.Collection;
import java.util.List;

public interface MTurk {
    void createHits(Collection<String> msg);

    List<SBtuple> processReviewablesHits();
}
