import mysql.connector
from datetime import datetime
import random
import time

HOST = "localhost"
PORT = 3306
DATABASE_NAME = "iot"
USERNAME = "root"
PASSWORD = "iot22-23"
JDBC_URL = f"mysql+mysqlconnector://{USERNAME}:{PASSWORD}@{HOST}:{PORT}/{DATABASE_NAME}"

oxygen_value = 99
cardio_value = 60
troponin_value = 0.01

OXYGEN_MIN = 60
CARDIO_MAX = 230
TROPONIN_MAX = 0.08

oxygen_increasing = False
cardio_increasing = True
troponin_increasing = True

def generate_values():
    global oxygen_value, cardio_value, troponin_value
    global oxygen_increasing, cardio_increasing, troponin_increasing

    oxygen_value += (1 if oxygen_increasing else -1) * random.random()
    if oxygen_value <= OXYGEN_MIN:
        oxygen_value = OXYGEN_MIN
        oxygen_increasing = True
    elif oxygen_value >= 99:
        oxygen_value = 99
        oxygen_increasing = False

    cardio_value += (1 if cardio_increasing else -1) * 5 * random.random()
    if cardio_value >= CARDIO_MAX:
        cardio_value = CARDIO_MAX
        cardio_increasing = False
    elif cardio_value <= 60:
        cardio_value = 60
        cardio_increasing = True

    troponin_value += (1 if troponin_increasing else -1) * 0.01
    if troponin_value >= TROPONIN_MAX:
        troponin_value = TROPONIN_MAX
        troponin_increasing = False
    elif troponin_value <= 0.01:
        troponin_value = 0.01
        troponin_increasing = True

def insert_into_database():
    connection = mysql.connector.connect(
        host=HOST,
        user=USERNAME,
        password=PASSWORD,
        database=DATABASE_NAME
    )
    cursor = connection.cursor()

    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    cursor.execute(f"INSERT INTO oxygen_sensor(id, timestamp, value) VALUES('o001', '{timestamp}', {int(oxygen_value)})")
    cursor.execute(f"INSERT INTO cardio_sensor(id, timestamp, value) VALUES('c001', '{timestamp}', {int(cardio_value)})")
    cursor.execute(f"INSERT INTO troponin_sensor(id, timestamp, value) VALUES('t001', '{timestamp}', {troponin_value:.2f})")

    connection.commit()
    cursor.close()
    connection.close()

if __name__ == "__main__":
    for _ in range(500):
        generate_values()
        insert_into_database()
        time.sleep(1)