const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const sgMail = require("@sendgrid/mail");
const moment = require('moment-timezone');


admin.initializeApp();
sgMail.setApiKey(functions.config().sendgrid.key);

exports.sendReportEmails = functions.pubsub
    .schedule("every Monday 09:00")
    .timeZone("America/Chicago")
    .onRun(async (context) => {
        const db = admin.database();
        const userRef = db.ref("users");

        try {
            const snapshot = await userRef.once("value");

            if (!snapshot.exists()) {
                console.log("No users found.");
                return null;
            }

            const users = snapshot.val();
            const sendEmailPromises = [];

            for (const uid in users) {
                const user = users[uid];
                const settings = user.userSettings || {};
                const email = user.email;
                if (settings.receiptsOn === true && email) {
                    
                    const tasks = user.tasks || {};

                    const tasksInfo = getTaskCountInfo(tasks);

                    const completedTasks = tasksInfo.total;
                    const highestDailyTasksDay = tasksInfo.day;
                    const highestDailyTasksAmount = tasksInfo.dayTasks
                    
                    const htmlContent = 
                    `<div style="font-family: Arial, sans-serif; padding: 20px;">
                            <h2>Weekly Report for ${email}</h2>

                            <p>Hello,</p>

                            <p>This email contains your user information pertaining to your account on our app <strong>DayPilot</strong>. We are providing your analytics and insights for this week.</p>

                            <p>This week you completed <strong>${completedTasks}</strong> tasks.</p>

                            <p>Your most productive day was <strong>${highestDailyTasksDay}</strong> where you completed <strong>${highestDailyTasksAmount}</strong> tasks.</p>

                            <p>Keep up the great work!</p>
                        </div>`;

                    const msg = {
                        to: email,
                        from: "app.daypilot@gmail.com",
                        subject: "Your Weekly Task Summary",
                        text: "Here is your weekly task summary from DayPilot.",
                        html: htmlContent,
                    };

                    sendEmailPromises.push(sgMail.send(msg));
                }
            }

            await Promise.all(sendEmailPromises);
            console.log(`Sent ${sendEmailPromises.length} emails.`);

            return null;
        } catch (error) {
            console.error("Error sending emails:", error);
            return null;
        }
    });

function getTaskCountInfo(tasks) {
    let totalTasksCompleted = 0;

    const today = new Date();

    const lastMonday = new Date(today);
    lastMonday.setDate(today.getDate() - 7);

    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);

    const formatDate = (date) =>
        date.getFullYear() + 
        '-' +
        String(date.getMonth() + 1).padStart(2, '0') +
        '-' +
        String(date.getDate()).padStart(2, '0');
        
    const lastMondayFormatted = formatDate(lastMonday);
    const yesterdayFormatted = formatDate(yesterday);
    
    const completedTasksPerDay = {};

    for (const taskId in tasks) {
        const task = tasks[taskId];
        const taskDate = task.date

        if (taskDate >= lastMondayFormatted && taskDate <= yesterdayFormatted) {
            if (task.completed === true) {
                if (!completedTasksPerDay[taskDate]) {
                    completedTasksPerDay[taskDate] = 0;
                }
                totalTasksCompleted++;
                completedTasksPerDay[taskDate]++;
            }
        }
    }

    let maxDay = null;
    let maxCount = 0;

    for (const day in completedTasksPerDay) {
        if (completedTasksPerDay[day] > maxCount) {
            maxCount = completedTasksPerDay[day];
            maxDay = day;
        }
    }

    return {day: maxDay, dayTasks: maxCount, total: totalTasksCompleted};
}

exports.deleteOldTasks = functions.pubsub
    .schedule("every 24 hours")
    .timeZone("America/Chicago")
    .onRun(async (context) => {

        const db = admin.database();
        const userRef = db.ref("users");

        try {
            const snapshot = await userRef.once("value");

            if (!snapshot.exists()) {
                console.log("No users found.");
                return null;
            }

            const users = snapshot.val();

            for (const uid in users) {

                const user = users[uid];
                
                const tasks = user.tasks || {};

                const today = new Date();

                const thirtyDaysAgo = new Date(today);
                thirtyDaysAgo.setDate(today.getDate() - 30);

                const formatDate = (date) =>
                    date.getFullYear() + 
                    '-' +
                    String(date.getMonth() + 1).padStart(2, '0') +
                    '-' +
                    String(date.getDate()).padStart(2, '0');
                    
                const thirtyDaysAgoFormatted = formatDate(thirtyDaysAgo);

                for (const taskId in tasks) {

                    const task = tasks[taskId];
                    const taskRef = db.ref(`users/${uid}/tasks/${taskId}`);

                    if (task.date < thirtyDaysAgoFormatted) {
                        taskRef.remove();
                    }
                }


            }
            return null;
        } catch (error) {
            console.error("Error deleting tasks:", error);
            return null;               
        }
    });

exports.unassignIncompleteTasks = functions.pubsub
    .schedule("every 1 minutes")
    .timeZone("UTC")
    .onRun(async (context) => {

        const db = admin.database();
        const userRef = db.ref("users");

        const snapshot = await userRef.once("value");

        const users = snapshot.val();

        for (const uid in users) {
            
            const user = users[uid];

            const tasks = user.tasks || {};

            for (const taskId in tasks) {

                if (task.endTime === "") {
                    continue;
                }

                const task = tasks[taskId];
                const taskRef = db.ref(`users/${uid}/tasks/${taskId}`);

                const taskEnd = moment.tz(`${task.date} ${task.endTime}`, "YYYY-MM-DD hh:mm A", user.timeZone).add(1, "hour");

                const taskUtc = taskEnd.toDate();

                if (taskUtc < new Date() && !task.completed) {

                    await taskRef.update({
                        date: "",
                        startTime: "",
                        endTime: ""
                    });
                }
            }

        }
    });

exports.setRepeatingTasks = functions.pubsub
    .schedule("every 1 minutes")
    .timeZone("UTC")
    .onRun(async (context) => {

        const db = admin.database();
        const userRef = db.ref("users");

        const snapshot = await userRef.once("value");

        const users = snapshot.val();

        for (const uid in users) {
            
            const user = users[uid];

            const tasks = user.tasks || {};

            const newTasks = [];
            

            for (const taskId in tasks) {

                const task = tasks[taskId];

                const taskDate = new Date(task.date);
                const now = new Date();

                const diffMs = taskDate - now;

                const diffDays = diffMs / (1000 * 60 * 60 * 24);

                const repeatsEnabled = Array.isArray(task.repeats) && task.repeats.some(Boolean);

                if (!repeatsEnabled) {

                    for (const tId in tasks) {

                        const t = tasks[tId];

                        if (t.generatedFrom === task.id) {
                            await db.ref(`users/${uid}/tasks/${tId}`).remove();
                        }
                    }
                    continue;
                }

                if (diffDays < 7) {
               
                    for (let i = 0; i < 7; i++) {
                        if (task.repeats[i]) {
                            const taskMoment = moment.tz(`${task.date} ${task.startTime}`, "YYYY-MM-DD hh:mm A", user.timeZone);
                            const dayofWeek = taskMoment.day();

                            let nextEvent = (i - dayofWeek + 7) % 7;

                            if (nextEvent === 0) {
                                nextEvent = 7;
                            }

                            for (let j = 0; j < 5; j++) {
                                const nextDate = taskMoment.clone().add(nextEvent, "days").add(7*j, "days");


                                const newTask = {
                                    completed: false,
                                    date: nextDate.format("YYYY-MM-DD"),
                                    description: task.description,
                                    endTime: task.endTime,
                                    id: task.id,
                                    repeats: task.repeats,
                                    startTime: task.startTime,
                                    title: task.title,
                                    generatedFrom: task.id,
                                    priority: task.priority
                                }

                                newTasks.push(newTask);
                            }
                            
                        }
                    }
                }
            }

            console.log(newTasks);

            for (const taskId in tasks) {

                const task = tasks[taskId];

                for (let i = newTasks.length - 1; i >= 0; i--) {

                    if (task.date === newTasks[i].date && task.id === newTasks[i].id)
                    {
                        newTasks.splice(i, 1);
                    }
                }
            }

            console.log(newTasks);

            for (let i = 0; i < newTasks.length; i++) {

                const taskRef = db.ref(`users/${uid}/tasks`).push();
                await taskRef.set(newTasks[i])
            }

        }
    })

    exports.setRepeatingTasksOnCall = functions.https.onCall(async (database, context) => {

        const { uid } = database;

        const db = admin.database();
        const userRef = db.ref(`users/${uid}`);

        const snapshot = await userRef.once("value");
        const user = snapshot.val();

        const tasks = user.tasks || {};
        const newTasks = [];

        for (const taskId in tasks) {

            const task = tasks[taskId];

            const taskDate = new Date(task.date);
            const now = new Date();

            const diffMs = taskDate - now;

            const diffDays = diffMs / (1000 * 60 * 60 * 24);

            const repeatsEnabled = Array.isArray(task.repeats) && task.repeats.some(Boolean);

            if (!repeatsEnabled) {

                for (const tId in tasks) {

                    const t = tasks[tId];

                    if (t.generatedFrom === task.id) {
                        await db.ref(`users/${uid}/tasks/${tId}`).remove();
                    }
                }
                continue;
            }

            if (diffDays < 7) {
            
                for (let i = 0; i < 7; i++) {
                    if (task.repeats[i]) {
                        const taskMoment = moment.tz(`${task.date} ${task.startTime}`, "YYYY-MM-DD hh:mm A", user.timeZone);
                        const dayofWeek = taskMoment.day();

                        let nextEvent = (i - dayofWeek + 7) % 7;

                        if (nextEvent === 0) {
                            nextEvent = 7;
                        }

                        for (let j = 0; j < 5; j++) {
                            const nextDate = taskMoment.clone().add(nextEvent, "days").add(7*j, "days");


                            const newTask = {
                                completed: false,
                                date: nextDate.format("YYYY-MM-DD"),
                                description: task.description,
                                endTime: task.endTime,
                                id: task.id,
                                repeats: task.repeats,
                                startTime: task.startTime,
                                title: task.title,
                                generatedFrom: task.id,
                                priority: task.priority
                            }

                            newTasks.push(newTask);
                        }
                        
                    }
                }
            }
        }


        for (const taskId in tasks) {

            const task = tasks[taskId];

            for (let i = newTasks.length - 1; i >= 0; i--) {

                if (task.date === newTasks[i].date && task.id === newTasks[i].id)
                {
                    newTasks.splice(i, 1);
                }
            }
        }


        for (let i = 0; i < newTasks.length; i++) {

            const taskRef = db.ref(`users/${uid}/tasks`).push();
            await taskRef.set(newTasks[i])
        }

        return { success: true, generated: newTasks.length };
    });
