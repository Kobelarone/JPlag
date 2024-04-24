# Constants
NUM_TEAMS = 4
TEAM_SIZE = 5
NUM_INDIVIDUALS = 20
NUM_EVENTS = 5

# Define event types
EVENT_TYPES = {
    1: "Team Event",
    2: "Individual Event"
}

# Points awarded for each event (to be decided)
EVENT_POINTS = {}

# Function to display menu options
def display_menu():
    print("\nWelcome To The Tournament Scoring System!\n")
    print("==========================================\n")
    print("[1.] Add Team")
    print("[2.] Add Individual")
    print("[3.] Add Event")
    print("[4.] View Score")
    print("[5.] Enter Single Event")
    print("[0.] Exit")
    print("\n==========================================\n")

# Function to add a team
def add_team():
    print("\nAdding Team...")
    team_name = input("Enter Team Name: ")
    print("Team", team_name, "has been registered!\n")

# Function to add an individual
def add_individual():
    print("\nAdding Individual...")
    name = input("Enter Name: ")
    team_name = input("Enter Team Name: ")
    print(name, "has been registered successfully!\n")

# Function to add an event
def add_event():
    print("\nAdding Event...")
    event = input("Enter Event Name: ")
    event_type = int(input("Is this a team event (1) or an individual event (2)?: "))
    print("Your", EVENT_TYPES.get(event_type, "Invalid Event Type"), ",", event, "has been added successfully!\n")

# Function to view score
def view_score():
    print("\nViewing Score...")
    num = int(input("Enter your score: "))
    teamscore = input("Did you (Win/Draw/Lose) your last game?: ")
    final_score = num + EVENT_POINTS.get(teamscore.title(), 0)
    print("Your final score is", final_score, "\n")

# Function to enter single event
def enter_single_event():
    print("\nEnter Single Event...")
    # Implement functionality for entering single event here
    pass

# Main function
def main():
    while True:
        display_menu()
        option = input("Please choose a menu option (0-5): ")
        if option == "0":
            print("Exiting Program. Goodbye!")
            break
        elif option == "1":
            add_team()
        elif option == "2":
            add_individual()
        elif option == "3":
            add_event()
        elif option == "4":
            view_score()
        elif option == "5":
            enter_single_event()
        else:
            print("Invalid option. Please choose a number from 0 to 5.\n")

# Run the program
if __name__ == "__main__":
    main()
