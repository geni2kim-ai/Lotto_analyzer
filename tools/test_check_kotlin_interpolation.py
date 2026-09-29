import unittest

from tools.check_kotlin_interpolation import find_violations_in_lines


class KotlinInterpolationCheckerTest(unittest.TestCase):

    def test_comment_markers_inside_string_do_not_start_comment_state(self):
        lines = [
            'val marker = "/* $drawNo회 */"',
            'val next = "$count건"',
        ]
        self.assertEqual(
            [(1, 'val marker = "/* $drawNo회 */"', "drawNo"),
             (2, 'val next = "$count건"', "count")],
            find_violations_in_lines(lines),
        )

    def test_real_block_comment_is_ignored_but_following_code_is_checked(self):
        lines = [
            "/*",
            ' * example "$drawNo회"',
            " */",
            'val live = "$count건"',
        ]
        self.assertEqual(
            [(4, 'val live = "$count건"', "count")],
            find_violations_in_lines(lines),
        )

    def test_nested_block_comments_are_ignored(self):
        lines = [
            "/* outer",
            "   /* inner $drawNo회 */",
            "   still comment $count건",
            "*/",
            'val safe = "${drawNo}회"',
        ]
        self.assertEqual([], find_violations_in_lines(lines))

    def test_triple_quoted_string_keeps_comment_markers_as_string_content(self):
        lines = [
            'val text = """',
            "/* $drawNo회 */",
            '"""',
        ]
        self.assertEqual(
            [(2, "/* $drawNo회 */", "drawNo")],
            find_violations_in_lines(lines),
        )

    def test_line_comment_is_ignored_after_executable_code(self):
        lines = [
            'val safe = "${drawNo}회" // "$count건"',
        ]
        self.assertEqual([], find_violations_in_lines(lines))


if __name__ == "__main__":
    unittest.main()
