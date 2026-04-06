package semo.back.service.feature.club.vo;

public enum ClubActivityTagKey {
    TENNIS(ClubActivityCategory.SPORTS),
    RUNNING(ClubActivityCategory.SPORTS),
    HIKING(ClubActivityCategory.SPORTS),
    CROSSFIT(ClubActivityCategory.SPORTS),
    CYCLING(ClubActivityCategory.SPORTS),
    SOCCER(ClubActivityCategory.SPORTS),
    FUTSAL(ClubActivityCategory.SPORTS),
    BASKETBALL(ClubActivityCategory.SPORTS),
    BADMINTON(ClubActivityCategory.SPORTS),
    SWIMMING(ClubActivityCategory.SPORTS),
    YOGA(ClubActivityCategory.SPORTS),
    PILATES(ClubActivityCategory.SPORTS),
    CLIMBING(ClubActivityCategory.SPORTS),
    GOLF(ClubActivityCategory.SPORTS),

    CODING(ClubActivityCategory.STUDY),
    ENGLISH(ClubActivityCategory.STUDY),
    LANGUAGE(ClubActivityCategory.STUDY),
    DESIGN(ClubActivityCategory.STUDY),
    MARKETING(ClubActivityCategory.STUDY),
    CAREER(ClubActivityCategory.STUDY),
    WRITING(ClubActivityCategory.STUDY),
    FINANCE(ClubActivityCategory.STUDY),
    CERTIFICATION(ClubActivityCategory.STUDY),
    AI(ClubActivityCategory.STUDY),
    DATA(ClubActivityCategory.STUDY),

    READING(ClubActivityCategory.CULTURE),
    MOVIE(ClubActivityCategory.CULTURE),
    MUSIC(ClubActivityCategory.CULTURE),
    BAND(ClubActivityCategory.CULTURE),
    PHOTOGRAPHY(ClubActivityCategory.CULTURE),
    DRAWING(ClubActivityCategory.CULTURE),
    ART(ClubActivityCategory.CULTURE),
    TRAVEL(ClubActivityCategory.CULTURE),
    FOOD(ClubActivityCategory.CULTURE),
    COFFEE(ClubActivityCategory.CULTURE),
    WINE(ClubActivityCategory.CULTURE),
    BOARD_GAME(ClubActivityCategory.CULTURE),
    PERFORMANCE(ClubActivityCategory.CULTURE),
    EXHIBITION(ClubActivityCategory.CULTURE),

    VOLUNTEERING(ClubActivityCategory.VOLUNTEER),
    ENVIRONMENT(ClubActivityCategory.VOLUNTEER),
    ANIMAL_CARE(ClubActivityCategory.VOLUNTEER),
    DONATION(ClubActivityCategory.VOLUNTEER),
    COMMUNITY_SERVICE(ClubActivityCategory.VOLUNTEER),
    EDUCATION_SUPPORT(ClubActivityCategory.VOLUNTEER),

    NETWORKING(ClubActivityCategory.NETWORKING),
    MEETUP(ClubActivityCategory.NETWORKING),
    COMMUNITY(ClubActivityCategory.NETWORKING),
    SOCIAL(ClubActivityCategory.NETWORKING),
    FREELANCER(ClubActivityCategory.NETWORKING),
    CREATOR(ClubActivityCategory.NETWORKING),
    PROFESSIONAL(ClubActivityCategory.NETWORKING);

    private final ClubActivityCategory category;

    ClubActivityTagKey(ClubActivityCategory category) {
        this.category = category;
    }

    public ClubActivityCategory category() {
        return category;
    }
}
