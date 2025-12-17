from flask import Flask, render_template, redirect

app = Flask(__name__)

@app.route('/')
def index():
    return redirect('/login')

@app.route('/login')
def login():
    return render_template('login.html')

@app.route('/chat')
def chat():
    return render_template('chat.html')

if __name__ == '__main__':
    print("Flask frontend running at http://localhost:5000")
    print("Open http://localhost:5000/login to start")
    app.run(port=5000, debug=True)
