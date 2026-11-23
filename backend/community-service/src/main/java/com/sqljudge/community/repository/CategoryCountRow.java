package com.sqljudge.community.repository;

import com.sqljudge.community.entity.PostCategory;

public interface CategoryCountRow {

    PostCategory getCategory();

    long getTotal();
}
