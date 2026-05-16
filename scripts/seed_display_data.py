#!/usr/bin/env python3

import json
import os
from contextlib import closing
from datetime import date, datetime, time, timedelta

import pymysql


PASSWORD_HASH = os.getenv("SEED_PASSWORD_HASH", "$2a$10$replace_with_bcrypt_hash")


def json_text(value):
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def at_day(days_ago, hour, minute):
    base = datetime.now() - timedelta(days=days_ago)
    return datetime.combine(base.date(), time(hour=hour, minute=minute))


class Seeder:
    def __init__(self):
        self.conn = pymysql.connect(
            host=os.getenv("COOK_DB_HOST", "127.0.0.1"),
            port=int(os.getenv("COOK_DB_PORT", "3306")),
            user=os.getenv("COOK_DB_USER", "root"),
            password=os.getenv("COOK_DB_PASSWORD", ""),
            database=os.getenv("COOK_DB_NAME", "cook"),
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor,
            autocommit=False,
        )

    def close(self):
        self.conn.close()

    def query_one(self, sql, params=None):
        with closing(self.conn.cursor()) as cur:
            cur.execute(sql, params or ())
            return cur.fetchone()

    def query_all(self, sql, params=None):
        with closing(self.conn.cursor()) as cur:
            cur.execute(sql, params or ())
            return cur.fetchall()

    def execute(self, sql, params=None):
        with closing(self.conn.cursor()) as cur:
            cur.execute(sql, params or ())
            return cur.lastrowid

    def ensure_user(self, payload):
        row = self.query_one("select id from users where phone = %s and deleted_at is null limit 1", (payload["phone"],))
        params = (
            PASSWORD_HASH,
            payload["nickname"],
            payload["avatar_url"],
            payload["gender"],
            payload["birthday"],
            payload["region"],
            payload["bio"],
            payload["status"],
            json_text(payload["interest_tags_json"]),
            json_text(payload["oauth_accounts_json"]),
            json_text(payload["stats_json"]),
            payload["created_at"],
            payload["updated_at"],
        )
        if row:
            self.execute(
                """
                update users
                set password_hash = %s,
                    nickname = %s,
                    avatar_url = %s,
                    gender = %s,
                    birthday = %s,
                    region = %s,
                    bio = %s,
                    status = %s,
                    interest_tags_json = %s,
                    oauth_accounts_json = %s,
                    stats_json = %s,
                    updated_at = %s
                where id = %s
                """,
                params[:-2] + (payload["updated_at"], row["id"]),
            )
            return row["id"]

        return self.execute(
            """
            insert into users (
                phone, password_hash, nickname, avatar_url, gender, birthday, region, bio, status,
                interest_tags_json, oauth_accounts_json, stats_json, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["phone"],
                *params,
            ),
        )

    def ensure_media(self, payload):
        row = self.query_one("select id from media_assets where original_name = %s and deleted_at is null limit 1", (payload["original_name"],))
        params = (
            payload["owner_id"],
            payload["biz_type"],
            payload["file_type"],
            payload["original_name"],
            payload["url"],
            payload["hls_url"],
            payload["status"],
            payload["size_bytes"],
            json_text(payload["metadata_json"]),
            payload["updated_at"],
        )
        if row:
            self.execute(
                """
                update media_assets
                set owner_id = %s,
                    biz_type = %s,
                    file_type = %s,
                    original_name = %s,
                    url = %s,
                    hls_url = %s,
                    status = %s,
                    size_bytes = %s,
                    metadata_json = %s,
                    updated_at = %s
                where id = %s
                """,
                params + (row["id"],),
            )
            return row["id"]

        return self.execute(
            """
            insert into media_assets (
                owner_id, biz_type, file_type, original_name, url, hls_url, status, size_bytes,
                metadata_json, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            params[:-1] + (payload["created_at"], payload["updated_at"]),
        )

    def ensure_recipe(self, payload):
        row = self.query_one(
            """
            select r.id as recipe_id, v.id as version_id
            from recipe_versions v
            inner join recipes r on r.id = v.recipe_id
            where r.author_id = %s and v.title = %s and v.deleted_at is null and r.deleted_at is null
            order by v.id asc
            limit 1
            """,
            (payload["author_id"], payload["title"]),
        )
        if row:
            recipe_id = row["recipe_id"]
            version_id = row["version_id"]
            self.execute(
                """
                update recipe_versions
                set cover_media_id = %s,
                    intro = %s,
                    difficulty = %s,
                    cook_time = %s,
                    serving = %s,
                    ingredients_json = %s,
                    steps_json = %s,
                    tips_json = %s,
                    video_json = %s,
                    status = 'published',
                    reject_reason = null,
                    updated_at = %s
                where id = %s
                """,
                (
                    payload["cover_media_id"],
                    payload["intro"],
                    payload["difficulty"],
                    payload["cook_time"],
                    payload["serving"],
                    json_text(payload["ingredients_json"]),
                    json_text(payload["steps_json"]),
                    json_text(payload["tips_json"]),
                    json_text(payload["video_json"]),
                    payload["updated_at"],
                    version_id,
                ),
            )
            self.execute(
                """
                update recipes
                set current_version_id = %s,
                    category_code = %s,
                    review_status = 'published',
                    publish_status = 'online',
                    updated_at = %s
                where id = %s
                """,
                (version_id, payload["category_code"], payload["updated_at"], recipe_id),
            )
            return recipe_id, version_id

        recipe_id = self.execute(
            """
            insert into recipes (
                author_id, current_version_id, category_code, review_status, publish_status,
                like_count, favorite_count, comment_count, created_at, updated_at
            ) values (%s, null, %s, 'published', 'online', 0, 0, 0, %s, %s)
            """,
            (
                payload["author_id"],
                payload["category_code"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )
        version_id = self.execute(
            """
            insert into recipe_versions (
                recipe_id, author_id, version_no, title, cover_media_id, intro, difficulty, cook_time, serving,
                ingredients_json, steps_json, tips_json, video_json, status, reject_reason, created_at, updated_at
            ) values (%s, %s, 1, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 'published', null, %s, %s)
            """,
            (
                recipe_id,
                payload["author_id"],
                payload["title"],
                payload["cover_media_id"],
                payload["intro"],
                payload["difficulty"],
                payload["cook_time"],
                payload["serving"],
                json_text(payload["ingredients_json"]),
                json_text(payload["steps_json"]),
                json_text(payload["tips_json"]),
                json_text(payload["video_json"]),
                payload["created_at"],
                payload["updated_at"],
            ),
        )
        self.execute(
            """
            update recipes
            set current_version_id = %s
            where id = %s
            """,
            (version_id, recipe_id),
        )
        return recipe_id, version_id

    def ensure_post(self, payload):
        row = self.query_one(
            "select id from posts where user_id = %s and content = %s and deleted_at is null limit 1",
            (payload["user_id"], payload["content"]),
        )
        params = (
            payload["recipe_id"],
            payload["media_ids_json"],
            payload["topic_codes_json"],
            payload["location"],
            payload["related_recipe_id"],
            payload["source_type"],
            payload["visibility"],
            payload["status"],
            payload["published_at"],
            payload["updated_at"],
            payload["content"],
            payload["user_id"],
        )
        if row:
            self.execute(
                """
                update posts
                set recipe_id = %s,
                    media_ids_json = %s,
                    topic_codes_json = %s,
                    location = %s,
                    related_recipe_id = %s,
                    source_type = %s,
                    visibility = %s,
                    status = %s,
                    published_at = %s,
                    updated_at = %s
                where content = %s
                  and user_id = %s
                  and id = %s
                """,
                params + (row["id"],),
            )
            return row["id"]

        return self.execute(
            """
            insert into posts (
                user_id, recipe_id, content, media_ids_json, topic_codes_json, location, related_recipe_id,
                source_type, location_json, visibility, status, like_count, favorite_count, comment_count,
                published_at, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0, 0, 0, %s, %s, %s)
            """,
            (
                payload["user_id"],
                payload["recipe_id"],
                payload["content"],
                payload["media_ids_json"],
                payload["topic_codes_json"],
                payload["location"],
                payload["related_recipe_id"],
                payload["source_type"],
                payload["location_json"],
                payload["visibility"],
                payload["status"],
                payload["published_at"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_comment(self, target_type, target_id, user_id, content, created_at, parent_id=None):
        row = self.query_one(
            """
            select id from comments
            where target_type = %s and target_id = %s and user_id = %s and content = %s and deleted_at is null
            limit 1
            """,
            (target_type, target_id, user_id, content),
        )
        if row:
            return row["id"]
        return self.execute(
            """
            insert into comments (
                target_type, target_id, user_id, parent_id, content, like_count, status, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, 0, 'normal', %s, %s)
            """,
            (target_type, target_id, user_id, parent_id, content, created_at, created_at),
        )

    def ensure_interaction(self, user_id, target_type, target_id, action_type, created_at):
        row = self.query_one(
            """
            select id from content_interactions
            where user_id = %s and target_type = %s and target_id = %s and action_type = %s
            limit 1
            """,
            (user_id, target_type, target_id, action_type),
        )
        if row:
            self.execute(
                """
                update content_interactions
                set status = 'active',
                    updated_at = %s
                where id = %s
                """,
                (created_at, row["id"]),
            )
            return row["id"]
        return self.execute(
            """
            insert into content_interactions (
                user_id, target_type, target_id, action_type, status, created_at, updated_at
            ) values (%s, %s, %s, %s, 'active', %s, %s)
            """,
            (user_id, target_type, target_id, action_type, created_at, created_at),
        )

    def ensure_checkin(self, payload):
        row = self.query_one(
            "select id from checkins where user_id = %s and checkin_date = %s and content = %s and deleted_at is null limit 1",
            (payload["user_id"], payload["checkin_date"], payload["content"]),
        )
        if row:
            self.execute(
                """
                update checkins
                set recipe_id = %s,
                    generated_post_id = %s,
                    media_ids_json = %s,
                    source_json = %s,
                    updated_at = %s
                where id = %s
                """,
                (
                    payload["recipe_id"],
                    payload["generated_post_id"],
                    payload["media_ids_json"],
                    payload["source_json"],
                    payload["updated_at"],
                    row["id"],
                ),
            )
            return row["id"]
        return self.execute(
            """
            insert into checkins (
                user_id, recipe_id, generated_post_id, checkin_date, content, media_ids_json, source_json, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["user_id"],
                payload["recipe_id"],
                payload["generated_post_id"],
                payload["checkin_date"],
                payload["content"],
                payload["media_ids_json"],
                payload["source_json"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_banner(self, payload):
        row = self.query_one("select id from banners where title = %s and deleted_at is null limit 1", (payload["title"],))
        params = (
            payload["subtitle"],
            payload["image_media_id"],
            payload["jump_type"],
            payload["jump_target"],
            payload["sort_no"],
            payload["status"],
            payload["start_at"],
            payload["end_at"],
            payload["updated_at"],
            payload["title"],
        )
        if row:
            self.execute(
                """
                update banners
                set subtitle = %s,
                    image_media_id = %s,
                    jump_type = %s,
                    jump_target = %s,
                    sort_no = %s,
                    status = %s,
                    start_at = %s,
                    end_at = %s,
                    updated_at = %s
                where title = %s
                  and id = %s
                """,
                params + (row["id"],),
            )
            return row["id"]
        return self.execute(
            """
            insert into banners (
                title, subtitle, image_media_id, jump_type, jump_target, sort_no, status, start_at, end_at,
                exposure_count, click_count, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0, 0, %s, %s)
            """,
            (
                payload["title"],
                payload["subtitle"],
                payload["image_media_id"],
                payload["jump_type"],
                payload["jump_target"],
                payload["sort_no"],
                payload["status"],
                payload["start_at"],
                payload["end_at"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_feedback(self, payload):
        row = self.query_one(
            "select id from feedbacks where user_id = %s and content = %s and deleted_at is null limit 1",
            (payload["user_id"], payload["content"]),
        )
        if row:
            self.execute(
                """
                update feedbacks
                set type = %s,
                    media_ids_json = %s,
                    contact = %s,
                    status = %s,
                    reply_content = %s,
                    replied_at = %s,
                    updated_at = %s
                where id = %s
                """,
                (
                    payload["type"],
                    payload["media_ids_json"],
                    payload["contact"],
                    payload["status"],
                    payload["reply_content"],
                    payload["replied_at"],
                    payload["updated_at"],
                    row["id"],
                ),
            )
            return row["id"]
        return self.execute(
            """
            insert into feedbacks (
                user_id, type, content, media_ids_json, contact, status, reply_content, replied_at, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["user_id"],
                payload["type"],
                payload["content"],
                payload["media_ids_json"],
                payload["contact"],
                payload["status"],
                payload["reply_content"],
                payload["replied_at"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_report(self, payload):
        row = self.query_one(
            """
            select id from reports
            where reporter_id = %s and target_type = %s and target_id = %s and reason_type = %s and deleted_at is null
            limit 1
            """,
            (payload["reporter_id"], payload["target_type"], payload["target_id"], payload["reason_type"]),
        )
        if row:
            self.execute(
                """
                update reports
                set reason = %s,
                    media_ids_json = %s,
                    status = %s,
                    handler_id = %s,
                    handle_result = %s,
                    handled_at = %s,
                    updated_at = %s
                where id = %s
                """,
                (
                    payload["reason"],
                    payload["media_ids_json"],
                    payload["status"],
                    payload["handler_id"],
                    payload["handle_result"],
                    payload["handled_at"],
                    payload["updated_at"],
                    row["id"],
                ),
            )
            return row["id"]
        return self.execute(
            """
            insert into reports (
                reporter_id, target_type, target_id, reason_type, reason, media_ids_json, status,
                handler_id, handle_result, handled_at, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["reporter_id"],
                payload["target_type"],
                payload["target_id"],
                payload["reason_type"],
                payload["reason"],
                payload["media_ids_json"],
                payload["status"],
                payload["handler_id"],
                payload["handle_result"],
                payload["handled_at"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_notification_conversation(self, user_id, created_at):
        row = self.query_one(
            "select id from conversations where type = 'notification' and target_id = %s and deleted_at is null limit 1",
            (user_id,),
        )
        if row:
            conversation_id = row["id"]
        else:
            conversation_id = self.execute(
                """
                insert into conversations (type, target_id, created_at, updated_at)
                values ('notification', %s, %s, %s)
                """,
                (user_id, created_at, created_at),
            )
        self.ensure_conversation_member(conversation_id, user_id, "system", 1, 0, 0, created_at)
        return conversation_id

    def ensure_private_conversation(self, user_a, user_b, created_at):
        row = self.query_one(
            """
            select c.id
            from conversations c
            inner join conversation_members m1 on m1.conversation_id = c.id and m1.user_id = %s and m1.status = 'normal'
            inner join conversation_members m2 on m2.conversation_id = c.id and m2.user_id = %s and m2.status = 'normal'
            where c.type = 'private' and c.deleted_at is null
            limit 1
            """,
            (user_a, user_b),
        )
        if row:
            conversation_id = row["id"]
        else:
            conversation_id = self.execute(
                """
                insert into conversations (type, target_id, created_at, updated_at)
                values ('private', null, %s, %s)
                """,
                (created_at, created_at),
            )
        self.ensure_conversation_member(conversation_id, user_a, "member", 1, 0, 1, created_at)
        self.ensure_conversation_member(conversation_id, user_b, "member", 0, 0, 0, created_at)
        return conversation_id

    def ensure_group_conversation(self, owner_id, name, intro, notice, member_ids, created_at):
        row = self.query_one(
            "select id, conversation_id from `groups` where owner_id = %s and name = %s and deleted_at is null limit 1",
            (owner_id, name),
        )
        if row:
            group_id = row["id"]
            conversation_id = row["conversation_id"]
            self.execute(
                """
                update `groups`
                set intro = %s,
                    notice = %s,
                    status = 'normal',
                    updated_at = %s
                where id = %s
                """,
                (intro, notice, created_at, group_id),
            )
            if not conversation_id:
                conversation_id = self.execute(
                    """
                    insert into conversations (type, target_id, created_at, updated_at)
                    values ('group', null, %s, %s)
                    """,
                    (created_at, created_at),
                )
                self.execute("update `groups` set conversation_id = %s where id = %s", (conversation_id, group_id))
        else:
            conversation_id = self.execute(
                """
                insert into conversations (type, target_id, created_at, updated_at)
                values ('group', null, %s, %s)
                """,
                (created_at, created_at),
            )
            group_id = self.execute(
                """
                insert into `groups` (
                    owner_id, conversation_id, name, avatar_media_id, intro, notice, status, created_at, updated_at
                ) values (%s, %s, %s, null, %s, %s, 'normal', %s, %s)
                """,
                (owner_id, conversation_id, name, intro, notice, created_at, created_at),
            )
        self.execute("update conversations set target_id = %s where id = %s", (group_id, conversation_id))
        self.ensure_conversation_member(conversation_id, owner_id, "owner", 0, 0, 1, created_at)
        for member_id in member_ids:
            if member_id != owner_id:
                self.ensure_conversation_member(conversation_id, member_id, "member", 1, 0, 0, created_at)
        return group_id, conversation_id

    def ensure_conversation_member(self, conversation_id, user_id, role, unread_count, is_muted, is_pinned, joined_at):
        row = self.query_one(
            """
            select id from conversation_members
            where conversation_id = %s and user_id = %s
            limit 1
            """,
            (conversation_id, user_id),
        )
        if row:
            self.execute(
                """
                update conversation_members
                set role = %s,
                    status = 'normal',
                    unread_count = %s,
                    is_muted = %s,
                    is_pinned = %s,
                    joined_at = %s,
                    updated_at = %s
                where id = %s
                """,
                (role, unread_count, is_muted, is_pinned, joined_at, joined_at, row["id"]),
            )
            return row["id"]
        return self.execute(
            """
            insert into conversation_members (
                conversation_id, user_id, role, status, unread_count, is_muted, is_pinned, joined_at, created_at, updated_at
            ) values (%s, %s, %s, 'normal', %s, %s, %s, %s, %s, %s)
            """,
            (conversation_id, user_id, role, unread_count, is_muted, is_pinned, joined_at, joined_at, joined_at),
        )

    def ensure_message(self, conversation_id, sender_id, message_type, content, created_at):
        content_json = json_text(content)
        row = self.query_one(
            """
            select id from messages
            where conversation_id = %s and message_type = %s and content_json = %s and coalesce(sender_id, 0) = %s and deleted_at is null
            limit 1
            """,
            (conversation_id, message_type, content_json, sender_id or 0),
        )
        if row:
            message_id = row["id"]
            self.execute(
                "update messages set updated_at = %s where id = %s",
                (created_at, message_id),
            )
        else:
            message_id = self.execute(
                """
                insert into messages (
                    conversation_id, sender_id, message_type, content_json, status, created_at, updated_at
                ) values (%s, %s, %s, %s, 'normal', %s, %s)
                """,
                (conversation_id, sender_id, message_type, content_json, created_at, created_at),
            )
        preview = "[图片]" if message_type == "image" else (content.get("content") if isinstance(content, dict) else "")
        self.execute(
            """
            update conversations
            set last_message_id = %s,
                last_message_preview = %s,
                last_message_at = %s,
                updated_at = %s
            where id = %s
            """,
            (message_id, preview, created_at, created_at, conversation_id),
        )
        return message_id

    def ensure_ai_message(self, payload):
        row = self.query_one(
            """
            select id from ai_messages
            where conversation_id = %s and user_id = %s and role = %s and content = %s and deleted_at is null
            limit 1
            """,
            (payload["conversation_id"], payload["user_id"], payload["role"], payload["content"]),
        )
        if row:
            self.execute(
                """
                update ai_messages
                set model_id = %s,
                    input_tokens = %s,
                    output_tokens = %s,
                    response_time_ms = %s,
                    flag = %s,
                    flag_reason = %s,
                    updated_at = %s
                where id = %s
                """,
                (
                    payload["model_id"],
                    payload["input_tokens"],
                    payload["output_tokens"],
                    payload["response_time_ms"],
                    payload["flag"],
                    payload["flag_reason"],
                    payload["updated_at"],
                    row["id"],
                ),
            )
            return row["id"]
        return self.execute(
            """
            insert into ai_messages (
                conversation_id, user_id, model_id, role, content, input_tokens, output_tokens, response_time_ms,
                flag, flag_reason, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["conversation_id"],
                payload["user_id"],
                payload["model_id"],
                payload["role"],
                payload["content"],
                payload["input_tokens"],
                payload["output_tokens"],
                payload["response_time_ms"],
                payload["flag"],
                payload["flag_reason"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_ai_recognition(self, payload):
        row = self.query_one(
            """
            select id from ai_image_recognition_logs
            where user_id = %s and image_url = %s and deleted_at is null
            limit 1
            """,
            (payload["user_id"], payload["image_url"]),
        )
        params = (
            payload["model_id"],
            payload["image_media_id"],
            payload["result_json"],
            payload["nutrition_json"],
            payload["suggestion"],
            payload["candidates_json"],
            payload["response_time_ms"],
            payload["error_message"],
            payload["status"],
            payload["recognized_name"],
            payload["confidence"],
            payload["calories"],
            payload["updated_at"],
        )
        if row:
            self.execute(
                """
                update ai_image_recognition_logs
                set model_id = %s,
                    image_media_id = %s,
                    result_json = %s,
                    nutrition_json = %s,
                    suggestion = %s,
                    candidates_json = %s,
                    response_time_ms = %s,
                    error_message = %s,
                    status = %s,
                    recognized_name = %s,
                    confidence = %s,
                    calories = %s,
                    updated_at = %s
                where id = %s
                """,
                params + (row["id"],),
            )
            return row["id"]
        return self.execute(
            """
            insert into ai_image_recognition_logs (
                user_id, model_id, image_media_id, image_url, result_json, nutrition_json, suggestion,
                candidates_json, response_time_ms, error_message, status, recognized_name, confidence, calories,
                created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["user_id"],
                *params[:2],
                payload["image_url"],
                *params[2:12],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def ensure_penalty(self, payload):
        row = self.query_one(
            """
            select id from user_penalties
            where user_id = %s and penalty_type = %s and reason = %s
            limit 1
            """,
            (payload["user_id"], payload["penalty_type"], payload["reason"]),
        )
        if row:
            self.execute(
                """
                update user_penalties
                set start_at = %s,
                    end_at = %s,
                    operator_id = %s,
                    updated_at = %s
                where id = %s
                """,
                (payload["start_at"], payload["end_at"], payload["operator_id"], payload["updated_at"], row["id"]),
            )
            return row["id"]
        return self.execute(
            """
            insert into user_penalties (
                user_id, penalty_type, reason, start_at, end_at, operator_id, created_at, updated_at
            ) values (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                payload["user_id"],
                payload["penalty_type"],
                payload["reason"],
                payload["start_at"],
                payload["end_at"],
                payload["operator_id"],
                payload["created_at"],
                payload["updated_at"],
            ),
        )

    def upsert_daily_statistics(self, stat_date, metric_json, created_at):
        row = self.query_one("select id from daily_statistics where stat_date = %s limit 1", (stat_date,))
        if row:
            self.execute(
                """
                update daily_statistics
                set metric_json = %s,
                    updated_at = %s
                where id = %s
                """,
                (json_text(metric_json), created_at, row["id"]),
            )
            return row["id"]
        return self.execute(
            """
            insert into daily_statistics (stat_date, metric_json, created_at, updated_at)
            values (%s, %s, %s, %s)
            """,
            (stat_date, json_text(metric_json), created_at, created_at),
        )

    def recalc_recipe_counts(self, recipe_id):
        self.execute(
            """
            update recipes
            set like_count = (
                    select count(1) from content_interactions
                    where target_type = 'recipe' and target_id = %s and action_type = 'like' and status = 'active'
                ),
                favorite_count = (
                    select count(1) from content_interactions
                    where target_type = 'recipe' and target_id = %s and action_type = 'favorite' and status = 'active'
                ),
                comment_count = (
                    select count(1) from comments
                    where target_type = 'recipe' and target_id = %s and deleted_at is null
                )
            where id = %s
            """,
            (recipe_id, recipe_id, recipe_id, recipe_id),
        )

    def recalc_post_counts(self, post_id):
        self.execute(
            """
            update posts
            set like_count = (
                    select count(1) from content_interactions
                    where target_type = 'post' and target_id = %s and action_type = 'like' and status = 'active'
                ),
                favorite_count = (
                    select count(1) from content_interactions
                    where target_type = 'post' and target_id = %s and action_type = 'favorite' and status = 'active'
                ),
                comment_count = (
                    select count(1) from comments
                    where target_type = 'post' and target_id = %s and deleted_at is null
                )
            where id = %s
            """,
            (post_id, post_id, post_id, post_id),
        )


def main():
    seeder = Seeder()
    summary = {
        "users": 0,
        "recipes": 0,
        "posts": 0,
        "comments": 0,
        "interactions": 0,
        "checkins": 0,
        "banners": 0,
        "feedbacks": 0,
        "reports": 0,
        "conversations": 0,
        "messages": 0,
        "ai_messages": 0,
        "ai_logs": 0,
        "penalties": 0,
        "daily_statistics": 0,
    }

    try:
        users = [
            {
                "phone": "13900010001",
                "nickname": "演示主厨阿禾",
                "avatar_url": "https://dummyimage.com/240x240/f59e0b/ffffff&text=A",
                "gender": "female",
                "birthday": date(1996, 5, 6),
                "region": "上海·静安",
                "bio": "偏爱轻食和快手便当，工作日晚餐尽量 30 分钟内完成。",
                "status": "normal",
                "interest_tags_json": ["健身餐", "轻食", "一人食"],
                "oauth_accounts_json": {},
                "stats_json": {"recipeCount": 3, "postCount": 3},
                "created_at": at_day(6, 9, 15),
                "updated_at": at_day(0, 10, 20),
            },
            {
                "phone": "13900010002",
                "nickname": "晚饭研究员老周",
                "avatar_url": "https://dummyimage.com/240x240/0f766e/ffffff&text=Z",
                "gender": "male",
                "birthday": date(1993, 11, 2),
                "region": "杭州·滨江",
                "bio": "主攻下饭菜和汤汁拌饭，喜欢把家常菜做出餐馆感。",
                "status": "normal",
                "interest_tags_json": ["家常菜", "晚餐", "炖菜"],
                "oauth_accounts_json": {},
                "stats_json": {"recipeCount": 1, "postCount": 1},
                "created_at": at_day(5, 10, 0),
                "updated_at": at_day(0, 10, 20),
            },
            {
                "phone": "13900010003",
                "nickname": "轻食教练Luna",
                "avatar_url": "https://dummyimage.com/240x240/4f46e5/ffffff&text=L",
                "gender": "female",
                "birthday": date(1994, 8, 12),
                "region": "深圳·南山",
                "bio": "帮学员准备低负担三餐，擅长做高蛋白便当。",
                "status": "normal",
                "interest_tags_json": ["高蛋白", "减脂", "便当"],
                "oauth_accounts_json": {},
                "stats_json": {"recipeCount": 1, "postCount": 1},
                "created_at": at_day(4, 14, 30),
                "updated_at": at_day(0, 10, 20),
            },
            {
                "phone": "13900010004",
                "nickname": "烘焙练习生Mia",
                "avatar_url": "https://dummyimage.com/240x240/db2777/ffffff&text=M",
                "gender": "female",
                "birthday": date(1998, 2, 18),
                "region": "成都·高新",
                "bio": "偏爱小甜品和低糖烘焙，正在练习更稳定的出品。",
                "status": "normal",
                "interest_tags_json": ["甜品", "烘焙", "低糖"],
                "oauth_accounts_json": {},
                "stats_json": {"recipeCount": 1, "postCount": 1},
                "created_at": at_day(3, 16, 45),
                "updated_at": at_day(0, 10, 20),
            },
        ]
        user_ids = {}
        for item in users:
            user_ids[item["phone"]] = seeder.ensure_user(item)
            summary["users"] += 1

        media_specs = [
            ("seed-banner-spring-light.png", user_ids["13900010001"], "https://dummyimage.com/1200x500/f6ad55/ffffff&text=Spring+Light"),
            ("seed-banner-dinner-plan.png", user_ids["13900010002"], "https://dummyimage.com/1200x500/f97316/ffffff&text=Dinner+Plan"),
            ("seed-banner-checkin-club.png", user_ids["13900010003"], "https://dummyimage.com/1200x500/10b981/ffffff&text=Checkin+Club"),
            ("seed-cover-energy-bowl.png", user_ids["13900010001"], "https://dummyimage.com/900x640/fbbf24/ffffff&text=Energy+Bowl"),
            ("seed-cover-beef-rice.png", user_ids["13900010002"], "https://dummyimage.com/900x640/f97316/ffffff&text=Beef+Rice"),
            ("seed-cover-bento-mushroom.png", user_ids["13900010003"], "https://dummyimage.com/900x640/14b8a6/ffffff&text=Bento"),
            ("seed-cover-matcha-cup.png", user_ids["13900010004"], "https://dummyimage.com/900x640/22c55e/ffffff&text=Matcha+Cup"),
            ("seed-cover-pumpkin-oat.png", user_ids["13900010001"], "https://dummyimage.com/900x640/60a5fa/ffffff&text=Pumpkin+Oat"),
        ]
        media_ids = {}
        for index, (name, owner_id, url) in enumerate(media_specs):
            media_ids[name] = seeder.ensure_media(
                {
                    "owner_id": owner_id,
                    "biz_type": "seed_display",
                    "file_type": "image",
                    "original_name": name,
                    "url": url,
                    "hls_url": None,
                    "status": "ready",
                    "size_bytes": 2048 + index,
                    "metadata_json": {"seed": True, "remote": True},
                    "created_at": at_day(2, 11, 0),
                    "updated_at": at_day(0, 10, 0),
                }
            )

        recipe_payloads = [
            {
                "author_id": user_ids["13900010001"],
                "title": "香煎鸡胸藜麦能量碗",
                "category_code": "fitness",
                "cover_media_id": media_ids["seed-cover-energy-bowl.png"],
                "intro": "鸡胸肉提前腌制后煎到微焦，搭配藜麦、玉米和牛油果，适合作为训练日的晚餐。",
                "difficulty": "简单",
                "cook_time": "25分钟",
                "serving": "1人份",
                "ingredients_json": [
                    {"name": "鸡胸肉", "amount": "180g"},
                    {"name": "熟藜麦", "amount": "120g"},
                    {"name": "玉米粒", "amount": "60g"},
                    {"name": "牛油果", "amount": "1/2个"},
                    {"name": "圣女果", "amount": "6颗"},
                    {"name": "黑胡椒", "amount": "少许"},
                ],
                "steps_json": [
                    {"title": "腌鸡胸肉", "desc": "鸡胸肉加入盐、黑胡椒和少量橄榄油抓匀，静置 10 分钟。", "imageUrl": "https://dummyimage.com/640x420/fbbf24/ffffff&text=Step+1"},
                    {"title": "煎至上色", "desc": "平底锅小火慢煎，两面都煎出浅焦色并完全熟透。", "imageUrl": "https://dummyimage.com/640x420/f59e0b/ffffff&text=Step+2"},
                    {"title": "组合能量碗", "desc": "碗中铺熟藜麦，依次摆上玉米、番茄、牛油果和切片鸡胸。", "imageUrl": "https://dummyimage.com/640x420/f97316/ffffff&text=Step+3"},
                ],
                "tips_json": [{"text": "鸡胸肉切开回温后再煎，更容易保持多汁。"}, {"text": "想增加饱腹感可以再搭配一枚水煮蛋。"}],
                "video_json": {"title": "能量碗快手版", "duration": "01:48", "status": "ready", "url": "https://example.com/videos/energy-bowl.m3u8", "coverUrl": "https://dummyimage.com/900x640/fbbf24/ffffff&text=Video"},
                "created_at": at_day(5, 18, 20),
                "updated_at": at_day(0, 10, 15),
            },
            {
                "author_id": user_ids["13900010002"],
                "title": "番茄牛腩拌饭",
                "category_code": "dinner",
                "cover_media_id": media_ids["seed-cover-beef-rice.png"],
                "intro": "一锅炖好的番茄牛腩拌热米饭特别稳，汤汁浓郁，适合工作日补能量。",
                "difficulty": "中等",
                "cook_time": "55分钟",
                "serving": "2人份",
                "ingredients_json": [
                    {"name": "牛腩", "amount": "450g"},
                    {"name": "番茄", "amount": "3个"},
                    {"name": "洋葱", "amount": "1/2个"},
                    {"name": "土豆", "amount": "1个"},
                    {"name": "米饭", "amount": "2碗"},
                ],
                "steps_json": [
                    {"title": "焯水去腥", "desc": "牛腩冷水下锅焯透，撇去浮沫后备用。", "imageUrl": "https://dummyimage.com/640x420/f97316/ffffff&text=Step+1"},
                    {"title": "炒出番茄底味", "desc": "番茄和洋葱炒软出汁，再加入牛腩翻匀。", "imageUrl": "https://dummyimage.com/640x420/f43f5e/ffffff&text=Step+2"},
                    {"title": "慢炖收浓", "desc": "加热水和土豆炖至牛腩软烂，最后大火收汁。", "imageUrl": "https://dummyimage.com/640x420/ea580c/ffffff&text=Step+3"},
                ],
                "tips_json": [{"text": "番茄先炒到起沙，汤底会更浓。"}, {"text": "米饭可以换成杂粮饭，整体更耐饿。"}],
                "video_json": {"title": "番茄牛腩拌饭", "duration": "03:22", "status": "ready", "url": "https://example.com/videos/beef-rice.m3u8"},
                "created_at": at_day(4, 19, 10),
                "updated_at": at_day(0, 10, 15),
            },
            {
                "author_id": user_ids["13900010003"],
                "title": "双椒杏鲍菇便当",
                "category_code": "bento",
                "cover_media_id": media_ids["seed-cover-bento-mushroom.png"],
                "intro": "杏鲍菇先煎再炒，口感像肉，和彩椒一起装进便当盒很提食欲。",
                "difficulty": "简单",
                "cook_time": "20分钟",
                "serving": "1人份",
                "ingredients_json": [
                    {"name": "杏鲍菇", "amount": "2根"},
                    {"name": "红椒", "amount": "1/2个"},
                    {"name": "青椒", "amount": "1/2个"},
                    {"name": "糙米饭", "amount": "1碗"},
                    {"name": "生抽", "amount": "1勺"},
                ],
                "steps_json": [
                    {"title": "切条煎香", "desc": "杏鲍菇切粗条，平底锅少油慢煎出边缘焦香。", "imageUrl": "https://dummyimage.com/640x420/10b981/ffffff&text=Step+1"},
                    {"title": "加入双椒快炒", "desc": "放入彩椒翻炒 2 分钟，淋生抽调味。", "imageUrl": "https://dummyimage.com/640x420/14b8a6/ffffff&text=Step+2"},
                    {"title": "装盒备餐", "desc": "搭配糙米饭和小番茄装盒，适合次日带饭。", "imageUrl": "https://dummyimage.com/640x420/0f766e/ffffff&text=Step+3"},
                ],
                "tips_json": [{"text": "杏鲍菇不要切太薄，煎后口感更好。"}],
                "video_json": {"title": "双椒杏鲍菇便当", "duration": "02:04", "status": "ready", "url": "https://example.com/videos/bento.m3u8"},
                "created_at": at_day(3, 12, 30),
                "updated_at": at_day(0, 10, 15),
            },
            {
                "author_id": user_ids["13900010004"],
                "title": "酸奶抹茶麻薯杯",
                "category_code": "dessert",
                "cover_media_id": media_ids["seed-cover-matcha-cup.png"],
                "intro": "低糖酸奶打底，抹茶麻薯软糯，冷藏后口感更清爽，适合下午茶。",
                "difficulty": "中等",
                "cook_time": "35分钟",
                "serving": "2杯",
                "ingredients_json": [
                    {"name": "无糖酸奶", "amount": "200g"},
                    {"name": "糯米粉", "amount": "80g"},
                    {"name": "抹茶粉", "amount": "6g"},
                    {"name": "牛奶", "amount": "110ml"},
                    {"name": "代糖", "amount": "适量"},
                ],
                "steps_json": [
                    {"title": "调麻薯糊", "desc": "糯米粉、牛奶和代糖搅匀，过筛后微波或蒸熟。", "imageUrl": "https://dummyimage.com/640x420/22c55e/ffffff&text=Step+1"},
                    {"title": "拌入抹茶", "desc": "麻薯稍微放凉后加入抹茶粉揉匀，剪成小块。", "imageUrl": "https://dummyimage.com/640x420/16a34a/ffffff&text=Step+2"},
                    {"title": "分层装杯", "desc": "酸奶、麻薯和燕麦脆分层装杯，冷藏 20 分钟后食用。", "imageUrl": "https://dummyimage.com/640x420/15803d/ffffff&text=Step+3"},
                ],
                "tips_json": [{"text": "抹茶粉先过筛，颜色会更均匀。"}, {"text": "想更像甜品店口感，可在顶部撒少量可可脆。"}],
                "video_json": {"title": "抹茶麻薯杯", "duration": "02:31", "status": "ready", "url": "https://example.com/videos/matcha-cup.m3u8"},
                "created_at": at_day(2, 15, 40),
                "updated_at": at_day(0, 10, 15),
            },
            {
                "author_id": user_ids["13900010001"],
                "title": "南瓜燕麦早餐杯",
                "category_code": "breakfast",
                "cover_media_id": media_ids["seed-cover-pumpkin-oat.png"],
                "intro": "蒸熟南瓜压泥后和燕麦、酸奶分层，早上从冰箱拿出来就能吃。",
                "difficulty": "简单",
                "cook_time": "15分钟",
                "serving": "1人份",
                "ingredients_json": [
                    {"name": "贝贝南瓜", "amount": "150g"},
                    {"name": "即食燕麦", "amount": "40g"},
                    {"name": "希腊酸奶", "amount": "120g"},
                    {"name": "蓝莓", "amount": "一小把"},
                ],
                "steps_json": [
                    {"title": "蒸熟南瓜", "desc": "南瓜切块蒸熟，压成细腻南瓜泥。", "imageUrl": "https://dummyimage.com/640x420/60a5fa/ffffff&text=Step+1"},
                    {"title": "分层入杯", "desc": "依次铺入南瓜泥、燕麦和酸奶。", "imageUrl": "https://dummyimage.com/640x420/3b82f6/ffffff&text=Step+2"},
                    {"title": "顶部点缀", "desc": "最后撒蓝莓和少量坚果即可。", "imageUrl": "https://dummyimage.com/640x420/2563eb/ffffff&text=Step+3"},
                ],
                "tips_json": [{"text": "前一晚做好冷藏，第二天口感更融合。"}],
                "video_json": {"title": "早餐杯", "duration": "01:20", "status": "ready", "url": "https://example.com/videos/pumpkin-oat.m3u8"},
                "created_at": at_day(1, 8, 5),
                "updated_at": at_day(0, 10, 15),
            },
        ]
        recipe_ids = {}
        for payload in recipe_payloads:
            recipe_id, _ = seeder.ensure_recipe(payload)
            recipe_ids[payload["title"]] = recipe_id
            summary["recipes"] += 1

        post_payloads = [
            {
                "user_id": user_ids["13900010001"],
                "recipe_id": recipe_ids["香煎鸡胸藜麦能量碗"],
                "content": "训练日的晚饭安排好了，鸡胸煎到微焦再配一点牛油果，饱腹感很稳。",
                "media_ids_json": json_text([media_ids["seed-cover-energy-bowl.png"]]),
                "topic_codes_json": json_text(["健身餐", "晚餐", "一人食"]),
                "location": "上海·静安",
                "related_recipe_id": recipe_ids["香煎鸡胸藜麦能量碗"],
                "source_type": "normal",
                "location_json": json_text({"city": "上海", "district": "静安"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(2, 19, 20),
                "created_at": at_day(2, 19, 10),
                "updated_at": at_day(0, 10, 18),
            },
            {
                "user_id": user_ids["13900010002"],
                "recipe_id": recipe_ids["番茄牛腩拌饭"],
                "content": "这锅番茄牛腩今天终于炖到满意了，汤汁拌饭真的不需要别的菜。",
                "media_ids_json": json_text([media_ids["seed-cover-beef-rice.png"]]),
                "topic_codes_json": json_text(["下饭菜", "晚餐"]),
                "location": "杭州·滨江",
                "related_recipe_id": recipe_ids["番茄牛腩拌饭"],
                "source_type": "normal",
                "location_json": json_text({"city": "杭州", "district": "滨江"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(1, 20, 5),
                "created_at": at_day(1, 19, 50),
                "updated_at": at_day(0, 10, 18),
            },
            {
                "user_id": user_ids["13900010003"],
                "recipe_id": recipe_ids["双椒杏鲍菇便当"],
                "content": "今天的带饭主题是高蛋白便当，杏鲍菇先煎后炒，口感特别像肉。",
                "media_ids_json": json_text([media_ids["seed-cover-bento-mushroom.png"]]),
                "topic_codes_json": json_text(["便当", "减脂"]),
                "location": "深圳·南山",
                "related_recipe_id": recipe_ids["双椒杏鲍菇便当"],
                "source_type": "normal",
                "location_json": json_text({"city": "深圳", "district": "南山"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(1, 12, 15),
                "created_at": at_day(1, 11, 58),
                "updated_at": at_day(0, 10, 18),
            },
            {
                "user_id": user_ids["13900010004"],
                "recipe_id": recipe_ids["酸奶抹茶麻薯杯"],
                "content": "抹茶麻薯杯冷藏过后更好吃，今天这一版终于把甜度压下来了。",
                "media_ids_json": json_text([media_ids["seed-cover-matcha-cup.png"]]),
                "topic_codes_json": json_text(["甜品", "烘焙"]),
                "location": "成都·高新",
                "related_recipe_id": recipe_ids["酸奶抹茶麻薯杯"],
                "source_type": "normal",
                "location_json": json_text({"city": "成都", "district": "高新"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(0, 15, 5),
                "created_at": at_day(0, 14, 48),
                "updated_at": at_day(0, 15, 10),
            },
            {
                "user_id": user_ids["13900010001"],
                "recipe_id": recipe_ids["南瓜燕麦早餐杯"],
                "content": "早餐打卡，南瓜燕麦杯从冰箱拿出来就能吃，出门前 3 分钟搞定。",
                "media_ids_json": json_text([media_ids["seed-cover-pumpkin-oat.png"]]),
                "topic_codes_json": json_text(["早餐", "打卡"]),
                "location": "上海·静安",
                "related_recipe_id": recipe_ids["南瓜燕麦早餐杯"],
                "source_type": "checkin",
                "location_json": json_text({"city": "上海", "district": "静安"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(0, 8, 20),
                "created_at": at_day(0, 8, 18),
                "updated_at": at_day(0, 8, 25),
            },
            {
                "user_id": user_ids["13900010001"],
                "recipe_id": None,
                "content": "本周备菜完成，冰箱里提前分装了鸡胸、烤南瓜和玉米，工作日会轻松很多。",
                "media_ids_json": json_text([media_ids["seed-cover-energy-bowl.png"], media_ids["seed-cover-pumpkin-oat.png"]]),
                "topic_codes_json": json_text(["备菜", "一周计划"]),
                "location": "上海·静安",
                "related_recipe_id": None,
                "source_type": "normal",
                "location_json": json_text({"city": "上海", "district": "静安"}),
                "visibility": "public",
                "status": "published",
                "published_at": at_day(0, 21, 10),
                "created_at": at_day(0, 20, 55),
                "updated_at": at_day(0, 21, 10),
            },
        ]
        post_ids = {}
        for payload in post_payloads:
            post_id = seeder.ensure_post(payload)
            post_ids[payload["content"]] = post_id
            summary["posts"] += 1

        checkins = [
            {
                "user_id": user_ids["13900010001"],
                "recipe_id": recipe_ids["南瓜燕麦早餐杯"],
                "generated_post_id": post_ids["早餐打卡，南瓜燕麦杯从冰箱拿出来就能吃，出门前 3 分钟搞定。"],
                "checkin_date": date.today(),
                "content": "早餐打卡，状态不错。",
                "media_ids_json": json_text([]),
                "source_json": json_text({"mealType": "breakfast", "calories": 360}),
                "created_at": at_day(0, 8, 15),
                "updated_at": at_day(0, 8, 25),
            },
            {
                "user_id": user_ids["13900010003"],
                "recipe_id": recipe_ids["双椒杏鲍菇便当"],
                "generated_post_id": None,
                "checkin_date": date.today() - timedelta(days=1),
                "content": "中午便当打卡，饱腹感刚好。",
                "media_ids_json": json_text([]),
                "source_json": json_text({"mealType": "lunch", "calories": 460}),
                "created_at": at_day(1, 12, 0),
                "updated_at": at_day(1, 12, 10),
            },
            {
                "user_id": user_ids["13900010002"],
                "recipe_id": recipe_ids["番茄牛腩拌饭"],
                "generated_post_id": None,
                "checkin_date": date.today() - timedelta(days=2),
                "content": "晚饭打卡，今天的炖菜很成功。",
                "media_ids_json": json_text([]),
                "source_json": json_text({"mealType": "dinner", "calories": 640}),
                "created_at": at_day(2, 20, 0),
                "updated_at": at_day(2, 20, 5),
            },
        ]
        for payload in checkins:
            seeder.ensure_checkin(payload)
            summary["checkins"] += 1

        comments = [
            ("recipe", recipe_ids["香煎鸡胸藜麦能量碗"], user_ids["13900010003"], "这个搭配很适合我准备训练前一天的晚餐。", at_day(1, 22, 0), None),
            ("recipe", recipe_ids["番茄牛腩拌饭"], user_ids["13900010001"], "汤汁看着就很适合拌饭，周末准备复刻。", at_day(1, 22, 20), None),
            ("post", post_ids["训练日的晚饭安排好了，鸡胸煎到微焦再配一点牛油果，饱腹感很稳。"], user_ids["13900010002"], "这个配色太舒服了，做得很干净。", at_day(1, 21, 0), None),
            ("post", post_ids["抹茶麻薯杯冷藏过后更好吃，今天这一版终于把甜度压下来了。"], user_ids["13900010001"], "这个抹茶颜色很漂亮，感觉很适合做下午茶。", at_day(0, 16, 0), None),
        ]
        for args in comments:
            seeder.ensure_comment(*args)
            summary["comments"] += 1

        interactions = [
            (user_ids["13900010002"], "recipe", recipe_ids["香煎鸡胸藜麦能量碗"], "like", at_day(1, 21, 5)),
            (user_ids["13900010003"], "recipe", recipe_ids["香煎鸡胸藜麦能量碗"], "favorite", at_day(1, 21, 8)),
            (user_ids["13900010001"], "recipe", recipe_ids["番茄牛腩拌饭"], "like", at_day(1, 21, 15)),
            (user_ids["13900010004"], "recipe", recipe_ids["酸奶抹茶麻薯杯"], "favorite", at_day(0, 16, 20)),
            (user_ids["13900010003"], "post", post_ids["训练日的晚饭安排好了，鸡胸煎到微焦再配一点牛油果，饱腹感很稳。"], "like", at_day(1, 21, 10)),
            (user_ids["13900010002"], "post", post_ids["训练日的晚饭安排好了，鸡胸煎到微焦再配一点牛油果，饱腹感很稳。"], "favorite", at_day(1, 21, 11)),
            (user_ids["13900010001"], "post", post_ids["抹茶麻薯杯冷藏过后更好吃，今天这一版终于把甜度压下来了。"], "like", at_day(0, 16, 15)),
            (user_ids["13900010002"], "user", user_ids["13900010001"], "follow", at_day(0, 11, 30)),
            (user_ids["13900010003"], "user", user_ids["13900010001"], "follow", at_day(0, 11, 31)),
            (user_ids["13900010004"], "user", user_ids["13900010003"], "follow", at_day(0, 11, 32)),
        ]
        for args in interactions:
            seeder.ensure_interaction(*args)
            summary["interactions"] += 1

        for recipe_id in recipe_ids.values():
            seeder.recalc_recipe_counts(recipe_id)
        for post_id in post_ids.values():
            seeder.recalc_post_counts(post_id)

        banner_payloads = [
            {
                "title": "七日轻食计划",
                "subtitle": "上新 5 道工作日也能完成的轻负担菜谱",
                "image_media_id": media_ids["seed-banner-spring-light.png"],
                "jump_type": "recipe",
                "jump_target": str(recipe_ids["香煎鸡胸藜麦能量碗"]),
                "sort_no": 5,
                "status": "online",
                "start_at": at_day(7, 0, 0),
                "end_at": at_day(-20, 23, 59),
                "created_at": at_day(2, 9, 0),
                "updated_at": at_day(0, 10, 0),
            },
            {
                "title": "下班晚饭灵感",
                "subtitle": "热汤热饭 30 分钟内上桌",
                "image_media_id": media_ids["seed-banner-dinner-plan.png"],
                "jump_type": "recipe",
                "jump_target": str(recipe_ids["番茄牛腩拌饭"]),
                "sort_no": 8,
                "status": "online",
                "start_at": at_day(7, 0, 0),
                "end_at": at_day(-20, 23, 59),
                "created_at": at_day(2, 9, 20),
                "updated_at": at_day(0, 10, 0),
            },
            {
                "title": "早餐打卡俱乐部",
                "subtitle": "连续记录一周早餐，养成稳定饮食节奏",
                "image_media_id": media_ids["seed-banner-checkin-club.png"],
                "jump_type": "post",
                "jump_target": str(post_ids["早餐打卡，南瓜燕麦杯从冰箱拿出来就能吃，出门前 3 分钟搞定。"]),
                "sort_no": 12,
                "status": "online",
                "start_at": at_day(7, 0, 0),
                "end_at": at_day(-20, 23, 59),
                "created_at": at_day(2, 9, 40),
                "updated_at": at_day(0, 10, 0),
            },
        ]
        for payload in banner_payloads:
            seeder.ensure_banner(payload)
            summary["banners"] += 1

        feedback_payloads = [
            {
                "user_id": user_ids["13900010001"],
                "type": "function",
                "content": "希望首页可以增加最近常做菜谱的快捷入口。",
                "media_ids_json": json_text([]),
                "contact": "13900010001",
                "status": "resolved",
                "reply_content": "已记录到下一轮首页交互优化中。",
                "replied_at": at_day(0, 9, 40),
                "created_at": at_day(1, 9, 15),
                "updated_at": at_day(0, 9, 40),
            },
            {
                "user_id": user_ids["13900010003"],
                "type": "bug",
                "content": "便当列表筛选后返回顶部不够明显，容易误以为没有刷新。",
                "media_ids_json": json_text([]),
                "contact": "13900010003",
                "status": "processing",
                "reply_content": None,
                "replied_at": None,
                "created_at": at_day(0, 10, 5),
                "updated_at": at_day(0, 10, 5),
            },
        ]
        for payload in feedback_payloads:
            seeder.ensure_feedback(payload)
            summary["feedbacks"] += 1

        report_payloads = [
            {
                "reporter_id": user_ids["13900010004"],
                "target_type": "post",
                "target_id": post_ids["本周备菜完成，冰箱里提前分装了鸡胸、烤南瓜和玉米，工作日会轻松很多。"],
                "reason_type": "spam",
                "reason": "误点测试举报，确认链路可用即可。",
                "media_ids_json": json_text([]),
                "status": "accepted",
                "handler_id": 1,
                "handle_result": "已确认是测试数据，记录后关闭。",
                "handled_at": at_day(0, 9, 10),
                "created_at": at_day(0, 9, 0),
                "updated_at": at_day(0, 9, 10),
            },
            {
                "reporter_id": user_ids["13900010002"],
                "target_type": "recipe",
                "target_id": recipe_ids["酸奶抹茶麻薯杯"],
                "reason_type": "misleading",
                "reason": "想验证举报列表展示，内容本身无问题。",
                "media_ids_json": json_text([]),
                "status": "pending",
                "handler_id": None,
                "handle_result": None,
                "handled_at": None,
                "created_at": at_day(0, 10, 30),
                "updated_at": at_day(0, 10, 30),
            },
        ]
        for payload in report_payloads:
            seeder.ensure_report(payload)
            summary["reports"] += 1

        notification_conversation = seeder.ensure_notification_conversation(user_ids["13900010001"], at_day(0, 8, 0))
        private_conversation = seeder.ensure_private_conversation(user_ids["13900010001"], user_ids["13900010002"], at_day(1, 18, 0))
        _, group_conversation = seeder.ensure_group_conversation(
            user_ids["13900010001"],
            "七日轻食打卡群",
            "一起记录早餐、午餐和晚餐，不拼极限，只拼稳定。",
            "每天晚上 10 点前交作业，互相提醒别断签。",
            [user_ids["13900010001"], user_ids["13900010002"], user_ids["13900010003"]],
            at_day(1, 18, 30),
        )
        summary["conversations"] += 3

        conversation_messages = [
            (notification_conversation, None, "system", {"content": "你的菜谱《南瓜燕麦早餐杯》已被首页推荐位收录。"}, at_day(0, 8, 40)),
            (notification_conversation, None, "system", {"content": "你收到 2 条新的互动提醒，记得去社区看看。"}, at_day(0, 21, 30)),
            (private_conversation, user_ids["13900010001"], "text", {"content": "番茄牛腩那锅你炖了多久？看起来很稳。"}, at_day(1, 18, 5)),
            (private_conversation, user_ids["13900010002"], "text", {"content": "差不多 45 分钟，小火慢炖，最后收汁就行。"}, at_day(1, 18, 7)),
            (private_conversation, user_ids["13900010001"], "text", {"content": "收到，我周末试一锅。"}, at_day(1, 18, 9)),
            (group_conversation, None, "system", {"content": "群聊已创建"}, at_day(1, 18, 30)),
            (group_conversation, user_ids["13900010003"], "text", {"content": "明天午餐我准备上传新的便当搭配。"}, at_day(1, 18, 45)),
            (group_conversation, user_ids["13900010001"], "text", {"content": "我先打个卡，早餐杯已经备好了。"}, at_day(0, 8, 35)),
            (group_conversation, user_ids["13900010002"], "text", {"content": "晚上我发番茄牛腩版本 2.0。"}, at_day(0, 20, 10)),
        ]
        for args in conversation_messages:
            seeder.ensure_message(*args)
            summary["messages"] += 1

        default_chat_model = seeder.query_one(
            "select id from ai_models where model_type = 'chat' and is_default = 1 and deleted_at is null order by id desc limit 1"
        )
        default_vision_model = seeder.query_one(
            "select id from ai_models where model_type = 'vision' and is_default = 1 and deleted_at is null order by id desc limit 1"
        )
        chat_model_id = default_chat_model["id"] if default_chat_model else 4
        vision_model_id = default_vision_model["id"] if default_vision_model else 2

        ai_messages = [
            {
                "conversation_id": 880001,
                "user_id": user_ids["13900010001"],
                "model_id": chat_model_id,
                "role": "user",
                "content": "晚上训练后吃鸡胸藜麦能量碗会不会太少？",
                "input_tokens": 28,
                "output_tokens": 0,
                "response_time_ms": 0,
                "flag": "normal",
                "flag_reason": None,
                "created_at": at_day(0, 22, 0),
                "updated_at": at_day(0, 22, 0),
            },
            {
                "conversation_id": 880001,
                "user_id": user_ids["13900010001"],
                "model_id": chat_model_id,
                "role": "assistant",
                "content": "如果训练强度中等，这份搭配基本够用。建议保留 180g 左右鸡胸肉，再加一份酸奶或水煮蛋，能更稳地补足蛋白质。",
                "input_tokens": 28,
                "output_tokens": 96,
                "response_time_ms": 1430,
                "flag": "normal",
                "flag_reason": None,
                "created_at": at_day(0, 22, 0),
                "updated_at": at_day(0, 22, 0),
            },
        ]
        for payload in ai_messages:
            seeder.ensure_ai_message(payload)
            summary["ai_messages"] += 1

        recognition_result = {
            "recognizedName": "香煎鸡胸藜麦能量碗",
            "confidence": 92.0,
            "calories": 498,
            "nutrition": {
                "protein": {"grams": 39, "percent": 34},
                "carbohydrate": {"grams": 42, "percent": 36},
                "fat": {"grams": 16, "percent": 30},
            },
            "candidates": [
                {"name": "香煎鸡胸藜麦能量碗", "confidence": 92},
                {"name": "鸡胸轻食碗", "confidence": 81},
                {"name": "藜麦鸡肉沙拉", "confidence": 74},
            ],
            "suggestion": "训练后 1 小时内吃这类餐食比较合适，如果当天活动量偏大，可以再补 1 份水果。",
            "disclaimer": "识别结果为估算值，仅供日常饮食参考。",
        }
        seeder.ensure_ai_recognition(
            {
                "user_id": user_ids["13900010001"],
                "model_id": vision_model_id,
                "image_media_id": media_ids["seed-cover-energy-bowl.png"],
                "image_url": "https://dummyimage.com/900x640/fbbf24/ffffff&text=Energy+Bowl",
                "result_json": json_text(recognition_result),
                "nutrition_json": json_text(recognition_result["nutrition"]),
                "suggestion": recognition_result["suggestion"],
                "candidates_json": json_text(recognition_result["candidates"]),
                "response_time_ms": 1680,
                "error_message": None,
                "status": "success",
                "recognized_name": recognition_result["recognizedName"],
                "confidence": 92.0,
                "calories": 498,
                "created_at": at_day(0, 22, 10),
                "updated_at": at_day(0, 22, 10),
            }
        )
        summary["ai_logs"] += 1

        penalties = [
            {
                "user_id": user_ids["13900010002"],
                "penalty_type": "warning",
                "reason": "测试后台处罚记录展示，不影响登录。",
                "start_at": at_day(2, 9, 0),
                "end_at": at_day(1, 9, 0),
                "operator_id": 1,
                "created_at": at_day(2, 9, 0),
                "updated_at": at_day(0, 10, 0),
            },
            {
                "user_id": user_ids["13900010004"],
                "penalty_type": "muted",
                "reason": "测试禁言记录展示，24 小时后自动恢复。",
                "start_at": at_day(0, 7, 0),
                "end_at": at_day(-1, 7, 0),
                "operator_id": 1,
                "created_at": at_day(0, 7, 0),
                "updated_at": at_day(0, 10, 0),
            },
        ]
        for payload in penalties:
            seeder.ensure_penalty(payload)
            summary["penalties"] += 1

        for i in range(7):
            target_date = date.today() - timedelta(days=6 - i)
            created_at = at_day(6 - i, 23, 0)
            metrics = {
                "newUsers": 2 + (i % 3),
                "activeUsers": 18 + i * 2,
                "newRecipes": 1 + (i % 2),
                "publishedRecipes": 2 + i,
                "newPosts": 3 + i,
                "publishedPosts": 2 + i,
                "messages": 8 + i * 3,
                "aiCalls": 4 + i,
            }
            seeder.upsert_daily_statistics(target_date, metrics, created_at)
            summary["daily_statistics"] += 1

        seeder.conn.commit()
        print(json.dumps(summary, ensure_ascii=False, indent=2))
    except Exception:
        seeder.conn.rollback()
        raise
    finally:
        seeder.close()


if __name__ == "__main__":
    main()
