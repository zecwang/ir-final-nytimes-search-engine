from flask import Flask
from flask_restful import Resource, Api
from gensim.models import KeyedVectors

app = Flask(__name__)
api = Api(app)

model = KeyedVectors.load_word2vec_format('../Supporting Materials/GoogleNews-vectors-negative300.bin', binary=True)
vocabulary = model.wv.vocab
filtered_vocab = ['Tueday', 'Thurday']


class Similarity(Resource):
    def get(self, w1, w2):
        try:
            result = str(model.similarity(w1, w2))
        except KeyError:
            result = '0'
        return result


class WordInVocabulary(Resource):
    def get(self, word):
        if word in filtered_vocab:
            return 0
        if word in vocabulary:
            return 1
        else:
            return 0


api.add_resource(Similarity, '/similarity/<w1>/<w2>')
api.add_resource(WordInVocabulary, '/vocabulary/<word>')

app.run(port='8090')
