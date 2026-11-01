package com.sqljudge.judge.sandbox;

import java.sql.SQLException;

interface SqlErrors {

    boolean isTimeout(SQLException exception);

    String messageOf(SQLException exception);
}
