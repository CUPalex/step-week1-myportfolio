// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

"use strict"

function initGame() {
    const questions = [
        "Who is the first Russian tsar?",
        "Who of these people has ever been to Sakhalin?",
        "Who is Checkov?",
        "Who is Pushkin?",
        "What is Sakhalin?",
        "What is the purpose of this quiz?",
    ];
    const rightAnswers = [
        "Ivan IV",
        "Checkov",
        "Russian writer",
        "Russian poet",
        "an island",
        "I like Russia!",
    ];
    const leftAnswers = [
        "Petr I",
        "Pushkin",
        "Russian scientist",
        "Russian musician",
        "a city",
        "I don't know!",
    ];

    // quizContainer is a section container after heading "Superfun quiz"
    const quizContainer = document.getElementById("quiz");
    quizContainer.querySelector("button").remove();

    // creating game elements and appending them to quizContainer
    const scoreElement = document.createElement("div");
    const question = document.createElement("div");
    const leftButton = document.createElement("button");
    const rightButton = document.createElement("button");
    const correctText = document.createElement("div");
    const wrongText = document.createElement("div");
    correctText.innerHTML = "Correct!";
    wrongText.innerHTML = "Wrong!";
    correctText.classList.add("invisible");
    wrongText.classList.add("invisible");
    question.classList.add("quiz-question");
    leftButton.classList.add("quiz-button");
    rightButton.classList.add("quiz-button");

    quizContainer.append(scoreElement);
    quizContainer.append(question);
    quizContainer.append(leftButton);
    quizContainer.append(rightButton);
    quizContainer.append(correctText);
    quizContainer.append(wrongText);

    let score = 0;
    let currentQuestion = 0;

    leftButton.addEventListener("click", (ev) => {
        wrongText.classList.remove("invisible");
        setTimeout(() => {
            wrongText.classList.add("invisible");
        }, 1000);

        currentQuestion += 1;
        displayQuestion(currentQuestion, questions, leftAnswers, rightAnswers, score, scoreElement, leftButton, rightButton, question, quizContainer);
    });
    rightButton.addEventListener("click", (ev) => {
        score += 1;
        correctText.classList.remove("invisible");
        setTimeout(() => {
            correctText.classList.add("invisible");
        }, 1000);

        currentQuestion += 1;
        displayQuestion(currentQuestion, questions, leftAnswers, rightAnswers, score, scoreElement, leftButton, rightButton, question, quizContainer);
    });
    // display first question
    displayQuestion(currentQuestion, questions, leftAnswers, rightAnswers, score, scoreElement, leftButton, rightButton, question, quizContainer);
}

function displayQuestion(currentQuestion, questions, leftAnswers, rightAnswers, score, scoreElement, leftButton, rightButton, question, quizContainer) {
    // if it was the last question
    if (currentQuestion == questions.length) {
        endGame(score, questions, quizContainer);
        return;
    }
    // show next question
    scoreElement.innerHTML = "Current score: " + score;
    question.innerHTML = questions[currentQuestion];
    leftButton.innerHTML = leftAnswers[currentQuestion];
    rightButton.innerHTML = rightAnswers[currentQuestion];
}

function endGame(score, questions, quizContainer) {
    // creating element with "thanks for playing" text
    const endText = document.createElement("div");
    endText.innerText = "Your score: " + score +
        "/" + questions.length + ". Thank you for playing! By the way, that's true, the right answer is always right :)";
    endText.classList.add("quizEnd");
    // filling quizContainer only with "thanks for playing" text
    quizContainer.innerHTML = "";
    quizContainer.append(endText);
}
