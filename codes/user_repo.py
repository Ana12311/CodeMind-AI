"""用户数据访问（示例代码，用于 CODE_REVIEW RAG 检索验证）。"""

import sqlite3


class UserRepository:
    def __init__(self, db_path):
        self.db_path = db_path

    def find_by_username(self, username):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        # SQL 拼接：注入风险
        query = "SELECT * FROM users WHERE username = '%s'" % username
        cursor.execute(query)
        row = cursor.fetchone()
        conn.close()
        return row
