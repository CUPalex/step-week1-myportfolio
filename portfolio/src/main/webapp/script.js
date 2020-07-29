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

// is called when page loads
document.addEventListener("DOMContentLoaded", () => {
    // set max-comments variable
    const DEFAULT_COMMENTS_NUMBER = 3;
    const MAX_COMMENTS_NUMBER = 10;
    let maxComments = DEFAULT_COMMENTS_NUMBER;
    // set event to change maxComments variable
    const maxCommentsElement = document.getElementById("max-comments");
    maxCommentsElement.addEventListener("change", (event) => {
        const inputMaxComments = parseInt(event.target.value);
        // validation of maxComments number
        if (isNaN(inputMaxComments)) {
            displayErrorInput(maxCommentsElement, "Please, write a number in Arabic numerals");
            return;
        }
        if (inputMaxComments < 0) {
            displayErrorInput(maxCommentsElement, "Please, write a positive number");
            return;
        }
        if (inputMaxComments > MAX_COMMENTS_NUMBER) {
            displayErrorInput(maxCommentsElement, `Please, write a number less than ${MAX_COMMENTS_NUMBER}`);
            return;
        }
        maxComments = inputMaxComments;
        // reload comments
        loadComments(maxComments);
    });
    // set event to delete comments
    const buttonDeleteComments = document.getElementById("comments-delete");
    buttonDeleteComments.addEventListener("click", deleteAllComments);
    // add validation to comment-add form
    const commentAddForm = document.getElementById("comment-add-form");
    commentAddForm.addEventListener("submit", commentAddFormValidate);
    // initially load comments
    loadComments(maxComments);
});

function commentAddFormValidate(event) {
    // if name field is empty
    if (event.target.elements["comment-owner"].value === "") {
        displayErrorInput(event.target.elements["comment-owner"], "Please, enter your name");
        event.preventDefault();
    }
    // if comment-text field is empty
    if (event.target.elements["comment-text"].value === "") {
        displayErrorInput(event.target.elements["comment-text"], "Please, enter your comment");
        event.preventDefault();
    }
}

function loadComments(maxComments) {
    // cursor if a parameter for server
    // it points to the next entity in query that will be loaded
    // we need it for pagination
    let cursor;
    // load comments from DataServlet and read them as json
    const load = function() {
        // include cursor in fetchURL
        let fetchURL = `/comments?maxcomments=${maxComments}`;
        if (cursor !== undefined) {
            fetchURL += `&cursor=${cursor}`;
        }
        // fetch data
        fetch(fetchURL).then((response) => (response.json())).then((json) => {
              const commentsContainer = document.querySelector(".comments-container");
              commentsContainer.innerHTML = "";
              json.comments.forEach((comment) => {
              commentsContainer.append(createCommentElement(comment));
              // update cursor value
              cursor = json.cursor;
            });
        });
    };
    load();
    // set event listeners for pagination
    const rightArrow = document.getElementById("pagination-right");
    rightArrow.addEventListener("click", load);
}

function deleteAllComments() {
    // delete all comments in CommentDeleteServlet and refresh comment section on page
    fetch("/delete-data", { method: "POST" }).then(() => loadComments(0));
}

function displayErrorInput(inputElement, errorString) {
    inputElement.classList.add("error-input");
    const errorMessage = document.createElement("div");
    errorMessage.classList.add("error-message");
    errorMessage.innerHTML = errorString;
    inputElement.after(errorMessage);
    const removeErrorDisplay = function() {
        inputElement.classList.remove("error-input");
        errorMessage.remove();
        inputElement.removeEventListener("click", removeErrorDisplay);
    };
    inputElement.addEventListener("click", removeErrorDisplay);
    setTimeout(removeErrorDisplay, 5000);
}

// creates and returns DOM comment-item element from class from datastore
function createCommentElement(comment) {
    const commentElement = document.createElement("div");
    commentElement.classList.add("comment-item");

    const commentOwner = document.createElement("div");
    commentOwner.classList.add("comment-owner");
    commentOwner.innerHTML = comment.commentOwner;
    commentElement.append(commentOwner);

    const commentDate = document.createElement("div");
    commentDate.classList.add("comment-date");
    // converts time in milliseconds to readable date string
    const date = new Date(comment.timestamp).toLocaleDateString();
    commentDate.innerHTML = date;
    commentElement.append(commentDate);

    const commentText = document.createElement("div");
    commentText.classList.add("comment-text");
    commentText.innerHTML = comment.commentText;
    commentElement.append(commentText);
    return commentElement;
}

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
    if (currentQuestion === questions.length) {
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
