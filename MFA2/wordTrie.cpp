#include <unordered_map> 
#include <string>       
#include <vector>
#include <iostream>
using namespace std;

class TrieNode {
public:
    unordered_map<char, TrieNode*> children;
    bool isEndOfWord;
    TrieNode() : isEndOfWord(false) {}
};

class Trie {
private:
    TrieNode* root;
public:
    Trie() {
        root = new TrieNode();
    }
    void insert(const string& word) {
        TrieNode* node = root;
        for (char ch : word) {
            if (node->children.find(ch) == node->children.end()) {
                node->children[ch] = new TrieNode();
            }
            node = node->children[ch];
        }
        node->isEndOfWord = true;
    }
    bool search(const string& word) {
        TrieNode* node = root;
        for (char ch : word) {
            if (node->children.find(ch) == node->children.end()) {
                return false;
            }
            node = node->children[ch];
        }
        return node->isEndOfWord;
    }
};

class Solution {
public:
    Trie trie;
    vector<string> wordBreak(string s, vector<string>& wordDict) {
        vector<string> res;
        for (size_t i = 0; i < wordDict.size(); i++) { // 使用 size_t
            trie.insert(wordDict[i]); 
        }
        string currentString = "";
        backtrack(s, 0, res, currentString);
        return res;
    }
    void backtrack(const string& s, size_t start, vector<string>& res, string currentString) { // 使用 size_t
        if (start == s.length()) { // 使用 size_t
            res.push_back(currentString);
            return;
        }

        string subStr = "";
        for (size_t i = start; i < s.length(); i++) { // 使用 size_t
            subStr += s[i];

            if (trie.search(subStr)) {
                string newString = currentString.empty() ? subStr : currentString + " " + subStr;
                backtrack(s, i + 1, res, newString);
            }
        }
    }
};

int main() {
    Solution solution;
    vector<string> word = {"apple", "pen", "applepen", "pine", "pineapple"};
    string s = "pineapplepenapple";
    vector<string> result = solution.wordBreak(s, word);

    for (const auto& sentence : result) {
        cout << sentence << endl;
    }
    return 0;
}