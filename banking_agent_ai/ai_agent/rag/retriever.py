from typing import List, Tuple
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

class SimpleRAG:
    def __init__(self, docs_dir: str):
        self.docs_dir = Path(docs_dir)
        self.docs = []
        self.names = []
        for p in sorted(self.docs_dir.glob("*.txt")):
            self.names.append(p.name)
            self.docs.append(p.read_text(encoding="utf-8", errors="ignore"))
        if self.docs:
            self.vectorizer = TfidfVectorizer(stop_words="english")
            self.matrix = self.vectorizer.fit_transform(self.docs)
        else:
            self.vectorizer = None
            self.matrix = None

    def query(self, q: str, top_k: int = 3) -> List[Tuple[str, str, float]]:
        if not self.docs or not self.vectorizer:
            return []
        qv = self.vectorizer.transform([q])
        sims = cosine_similarity(qv, self.matrix)[0]
        idxs = sims.argsort()[::-1][:top_k]
        result = []
        for i in idxs:
            result.append((self.names[i], self.docs[i][:800], float(sims[i])))
        return result
