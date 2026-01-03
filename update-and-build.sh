#!/bin/bash

# JIPTV Update and Build Script
# Usage: ./update-and-build.sh

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to get available branches
get_branches() {
    git branch -r | grep -v HEAD | sed 's/origin\///' | sed 's/^[[:space:]]*//' | sort
}

# Function to show available branches with numbers
show_branches() {
    local branches=("$@")
    print_info "Available branches:"
    for i in "${!branches[@]}"; do
        local current_marker=""
        if [ "${branches[$i]}" = "$(git branch --show-current)" ]; then
            current_marker=" ${GREEN}(current)${NC}"
        fi
        printf "  ${BLUE}%2d)${NC} %s%s\n" $((i+1)) "${branches[$i]}" "$current_marker"
    done
}

# Function to select branch
select_branch() {
    echo
    print_info "Current branch: $(git branch --show-current)"
    echo
    
    # Get available branches into array
    local branches=()
    while IFS= read -r branch; do
        branches+=("$branch")
    done < <(get_branches)
    
    if [ ${#branches[@]} -eq 0 ]; then
        print_error "No remote branches found!"
        exit 1
    fi
    
    # Show branches with numbers
    show_branches "${branches[@]}"
    echo
    
    # Ask user for selection
    while true; do
        read -p "Select branch by number (1-${#branches[@]}) or press Enter for current branch: " selection
        
        # If empty, use current branch
        if [ -z "$selection" ]; then
            selected_branch=$(git branch --show-current)
            print_info "Using current branch: $selected_branch"
            break
        fi
        
        # Check if input is a valid number
        if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le ${#branches[@]} ]; then
            selected_branch="${branches[$((selection-1))]}"
            print_info "Selected branch: $selected_branch"
            break
        else
            print_error "Invalid selection. Please enter a number between 1 and ${#branches[@]}"
        fi
    done
    
    echo "$selected_branch"
}

# Function to update repository
update_repository() {
    local branch=$1
    
    print_info "Updating repository..."
    
    # Fetch latest changes
    print_info "Fetching latest changes from origin..."
    git fetch origin
    
    # Check if branch exists
    if ! git show-ref --verify --quiet refs/remotes/origin/$branch; then
        print_error "Branch '$branch' does not exist on remote!"
        exit 1
    fi
    
    # Switch to branch if not already on it
    current_branch=$(git branch --show-current)
    if [ "$current_branch" != "$branch" ]; then
        print_info "Switching to branch: $branch"
        git checkout $branch
    fi
    
    # Pull latest changes
    print_info "Pulling latest changes..."
    git pull origin $branch
    
    print_success "Repository updated successfully!"
}

# Function to build Docker image
build_docker_image() {
    print_info "Building Docker image: jiptv:latest"
    
    # Check if Dockerfile exists
    if [ ! -f "Dockerfile" ]; then
        print_error "Dockerfile not found in current directory!"
        exit 1
    fi
    
    # Build the image
    print_info "Starting Docker build process..."
    docker build -t jiptv:latest .
    
    if [ $? -eq 0 ]; then
        print_success "Docker image built successfully!"
        
        # Show image info
        print_info "Image details:"
        docker images jiptv:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    else
        print_error "Docker build failed!"
        exit 1
    fi
}

# Function to show current status
show_status() {
    print_info "Current status:"
    echo "  Repository: $(pwd)"
    echo "  Branch: $(git branch --show-current)"
    echo "  Last commit: $(git log -1 --pretty=format:'%h - %s (%cr)')"
    echo
}

# Function to restart application (optional)
restart_application() {
    echo
    read -p "Do you want to restart the JIPTV application in Portainer? (y/N): " restart_choice
    
    if [[ $restart_choice =~ ^[Yy]$ ]]; then
        print_info "To restart the application:"
        echo "1. Go to Portainer: https://dock.mallepetrus.nl"
        echo "2. Navigate to Stacks → jiptv-app"
        echo "3. Click 'Restart' or 'Update the stack'"
        echo
        print_warning "Note: The new image will be used after restart!"
    fi
}

# Main script execution
main() {
    clear
    echo "=================================================="
    echo "🚀 JIPTV Update and Build Script"
    echo "=================================================="
    echo
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository!"
        exit 1
    fi
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH!"
        exit 1
    fi
    
    # Show current status
    show_status
    
    # Select branch
    selected_branch=$(select_branch)
    
    # Confirm action
    echo
    print_warning "This will:"
    echo "  1. Switch to branch: $selected_branch"
    echo "  2. Pull latest changes"
    echo "  3. Build Docker image: jiptv:latest"
    echo
    read -p "Continue? (y/N): " confirm
    
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "Operation cancelled."
        exit 0
    fi
    
    echo
    print_info "Starting update and build process..."
    echo
    
    # Update repository
    update_repository "$selected_branch"
    echo
    
    # Build Docker image
    build_docker_image
    echo
    
    print_success "Update and build completed successfully!"
    
    # Optional restart
    restart_application
    
    echo
    print_success "All done! 🎉"
}

# Run main function
main "$@"