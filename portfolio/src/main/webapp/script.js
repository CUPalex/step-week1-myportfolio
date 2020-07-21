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

/**
 * Adds a random greeting to the page.
 */
'use strict'

function start() {
    const questions = [
        "Where is the Moscow State University situalted?",
        "Why was the previous question about MSU?",
        "Who of these people has ever been to Sakhalin?",
        "Who is Checkov?",
        "Who is Pushkin?",
        "Wht is Sakhalin?",
        "What is the purpose of this quiz?",
    ];
    const rightAnswers = [
        "Russia",
        "I only have been there once or twice",
        "Checkov",
        "Russian writer",
        "Russian poet",
        "an island",
        "Ugh..",
    ];
    const leftAnswers = [
        "Moscow",
        "I study there",
        "Pushkin",
        "Russian scientist",
        "Russian musician",
        "a city",
        "I don't know!",
    ];

    const quizContainer = document.getElementById("quiz");
    quizContainer.querySelector("button").remove();

    let score = 0;
    let scoreElement = document.createElement("div");
    let question = document.createElement("div");
    let leftButton = document.createElement("button");
    let rightButton = document.createElement("button");
    let correctText = document.createElement("div");
    let wrongText = document.createElement("div");
    correctText.innerHTML = "Correct!";
    wrongText.innerHTML = "Wrong!";
    correctText.classList.add("invisible");
    wrongText.classList.add("invisible");
    scoreElement.classList.add("quizScore");
    question.classList.add("quizQuestion");
    leftButton.classList.add("quizButton");
    rightButton.classList.add("quizButton");
    correctText.classList.add("quizResult");
    wrongText.classList.add("quizResult");


    quizContainer.append(scoreElement);
    quizContainer.append(question);
    quizContainer.append(leftButton);
    quizContainer.append(rightButton);
    quizContainer.append(correctText);
    quizContainer.append(wrongText);

    let currentQuestion = 0;

    leftButton.addEventListener("click", (ev) => {
        wrongText.classList.remove("invisible");
        setTimeout(() => {
            wrongText.classList.add("invisible");
        }, 1000);

        currentQuestion += 1;
        if (currentQuestion >= questions.length) {
            let endText = document.createElement("div");
            endText.innerText = `Your score: ` + score +
                `/` + questions.length + `. Thank you for playing! By the way, that's true, the right answer is always right :)`;
            endText.classList.add("quizEnd");
            quizContainer.innerHTML = "";
            quizContainer.append(endText);
            return;
        }
        scoreElement.innerHTML = "Current score: " + score;
        question.innerHTML = questions[currentQuestion];
        leftButton.innerHTML = leftAnswers[currentQuestion];
        rightButton.innerHTML = rightAnswers[currentQuestion];
    });
    rightButton.addEventListener("click", (ev) => {
        score += 1;
        correctText.classList.remove("invisible");
        setTimeout(() => {
            correctText.classList.add("invisible");
        }, 1000);

        currentQuestion += 1;
        if (currentQuestion >= questions.length) {
            let endText = document.createElement("div");
            endText.innerText = `Your score: ` + score +
                `/` + questions.length + `. Thank you for playing! By the way, that's true, the right answer is always right :)`;
            endText.classList.add("quizEnd");
            quizContainer.innerHTML = "";
            quizContainer.append(endText);
            return;
        }
        scoreElement.innerHTML = "Current score: " + score;
        question.innerHTML = questions[currentQuestion];
        leftButton.innerHTML = leftAnswers[currentQuestion];
        rightButton.innerHTML = rightAnswers[currentQuestion];
    });

    scoreElement.innerHTML = "Current score: " + score;
    question.innerHTML = questions[currentQuestion];
    leftButton.innerHTML = leftAnswers[currentQuestion];
    rightButton.innerHTML = rightAnswers[currentQuestion];
}
